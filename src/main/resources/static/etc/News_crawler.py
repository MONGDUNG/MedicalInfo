#!/usr/bin/env python
# coding: utf-8

# In[2]:


pip install selenium


# In[3]:


pip install beautifulsoup4


# In[6]:


pip install pandas


# In[8]:


pip install webdriver-manager


# In[60]:


from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup
import pandas as pd
import json

# 크롬 옵션 설정
options = webdriver.ChromeOptions()
# options.add_argument('--headless')  # 창 띄우고 디버깅 가능하게
options.add_argument('--no-sandbox')
options.add_argument('--disable-dev-shm-usage')
options.add_argument('--window-size=1920,1080')

# 브라우저 실행
driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=options)
driver.get("https://www.mohw.go.kr/board.es?mid=a10503000000&bid=0027")

# DOM 로딩 대기
WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.CSS_SELECTOR, "a.txt_title"))
)

# HTML 저장 (디버깅용)
html = driver.page_source
with open("debug_output.html", "w", encoding="utf-8") as f:
    f.write(html)

driver.quit()

# 파싱
soup = BeautifulSoup(html, "html.parser")

# ✅ 제목에서 '새글' 문구 제거
titles = [a.text.replace("새글", "").strip() for a in soup.select("a.txt_title")]
dates = [d.text.strip() for d in soup.select('td[data-label="등록일"]')]
links = ["https://www.mohw.go.kr" + a['href'] for a in soup.select("a.txt_title")]

print("제목 수:", len(titles))
print("날짜 수:", len(dates))
print("링크 수:", len(links))

# 저장
data = {"title": titles, "date": dates, "link": links}
df = pd.DataFrame(data)

df.to_csv("mohw_news.csv", index=False, encoding="utf-8-sig")
with open("C:/Users/14A/mohw_news.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print("✅ 불필요한 문구 제거 완료 + 크롤링 성공 🎯")


# In[62]:


get_ipython().system('jupyter nbconvert --to script News_crawler.ipynb')

