"""Load normalized facilities into a NEW Elasticsearch index; never replaces an index."""
import argparse
import json
import os
from pathlib import Path
import re
import urllib.request

def ingest(url, index, source):
    if not re.fullmatch('[a-z0-9][a-z0-9_-]{0,127}', index):
        raise ValueError('Invalid index name')
    def request(method, path, payload=None, ndjson=False):
        headers={'Content-Type':'application/x-ndjson' if ndjson else 'application/json'}
        key=os.environ.get('ELASTICSEARCH_API_KEY')
        if key: headers['Authorization']='ApiKey '+key
        data=payload.encode() if isinstance(payload,str) else json.dumps(payload).encode() if payload is not None else None
        with urllib.request.urlopen(urllib.request.Request(url.rstrip('/')+'/'+path,data=data,headers=headers,method=method),timeout=60) as response:
            return json.load(response)
    mapping={
        'settings':{'number_of_shards':1,'number_of_replicas':0},
        'mappings':{'dynamic':'strict','properties':{
            **{name:{'type':'text','analyzer':'nori'} for name in ('NAME','ADDRESS','DEPT_NAME')},
            **{name:{'type':'keyword'} for name in ('ID','CATEGORY_NAME','PHONE','SOURCE','SOURCE_PERIOD')},
            'LAT':{'type':'double'},'LNG':{'type':'double'},'geoLocation':{'type':'geo_point'},
            'RAW_BASIC':{'type':'object','enabled':False},'RAW_DETAIL':{'type':'object','enabled':False}}}}
    request('PUT',index,mapping) # Existing index fails here, before writing any documents.
    total=0
    batch=[]
    def flush():
        result=request('POST',index+'/_bulk','\n'.join(batch)+'\n',True)
        if result.get('errors'):
            raise RuntimeError('Bulk import failed; do not switch to this index.')
    with source.open(encoding='utf-8') as stream:
        for line in stream:
            row=json.loads(line)
            batch.extend((json.dumps({'create':{'_id':row['ID']}}),line.strip()))
            total+=1
            if len(batch)>=1000:
                flush(); batch=[]
                if total%10000==0: print(f'Imported {total}',flush=True)
    if batch: flush()
    request('POST',index+'/_refresh')
    count=request('GET',index+'/_count')['count']
    if count != total: raise RuntimeError('Index count mismatch')
    print(f'Verified {count} documents in {index}',flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url',required=True)
    parser.add_argument('--index',required=True)
    parser.add_argument('--source',type=Path,required=True)
    args=parser.parse_args()
    try: ingest(args.url,args.index,args.source)
    except Exception:
        print('Import failed; original indexes are unchanged. Check connection, nori plugin, source and destination index.')
        raise SystemExit(1)
