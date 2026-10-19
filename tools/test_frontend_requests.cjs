const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const root = path.join(__dirname, '../dotnet/MedicalInfo.Web/wwwroot');
// The page only registers listeners at startup. No browser or DOM implementation
// is needed to exercise its actual HTTP helper and global declarations.
function load(script, fetch, extra = {}) {
  const control = { addEventListener() {} };
  const context = vm.createContext({
    document: { querySelector: () => control, querySelectorAll: () => [], body: { dataset: {} } },
    fetch, ...extra
  });
  vm.runInContext(fs.readFileSync(path.join(root, script), 'utf8'), context);
  return context;
}

test('catalog preserves successful JSON responses', async () => {
  const body = { total: 1, items: [{ id: 'official-product' }] };
  const page = load('drugs.js', async () => ({ ok: true, status: 200, json: async () => body }));
  assert.equal(await page.request('/api/drugs'), body);
});
test('empty rate-limit response produces a Korean retry message', async () => {
  const page = load('drugs.js', async () => ({ ok: false, status: 429, json: async () => { throw new SyntaxError('Empty body'); } }));
  await assert.rejects(page.request('/api/drugs'), /요청이 많습니다.*다시 검색/);
});
test('catalog keeps a server problem explanation', async () => {
  const page = load('drugs.js', async () => ({ ok: false, status: 503, json: async () => ({ title: '공식 제품 데이터를 준비 중입니다.' }) }));
  await assert.rejects(page.request('/api/drugs'), /공식 제품 데이터를 준비 중/);
});
test('non-JSON service outage produces a usable message', async () => {
  const page = load('drugs.js', async () => ({ ok: false, status: 503, json: async () => { throw new SyntaxError('HTML proxy error'); } }));
  await assert.rejects(page.request('/api/drugs'), /검색 서비스를 일시적으로 이용할 수 없습니다/);
});
test('malformed success does not expose a JSON parser exception', async () => {
  const page = load('drugs.js', async () => ({ ok: true, status: 200, json: async () => { throw new SyntaxError('Invalid JSON'); } }));
  await assert.rejects(page.request('/api/drugs'), /응답을 확인할 수 없습니다/);
});
test('network failure produces a Korean connection message', async () => {
  const page = load('drugs.js', async () => { throw new TypeError('Failed to fetch'); });
  await assert.rejects(page.request('/api/drugs'), /서버에 연결할 수 없습니다/);
});
test('map script preserves the browser scrollTo function used by Leaflet', () => {
  const nativeScrollTo = () => {};
  const page = load('search.js', undefined, { scrollTo: nativeScrollTo });
  assert.equal(page.scrollTo, nativeScrollTo);
});
