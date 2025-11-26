#!/usr/bin/env python3
import psycopg2

LOCAL_DB = {
    'host': 'localhost',
    'port': 5432,
    'database': 'aiinsight',
    'user': 'aiinsight',
    'password': 'aiinsight123'
}

try:
    conn = psycopg2.connect(**LOCAL_DB)
    cur = conn.cursor()
    
    print("📋 news_article 테이블 스키마:")
    cur.execute("""
        SELECT column_name, data_type 
        FROM information_schema.columns 
        WHERE table_name = 'news_article'
        ORDER BY ordinal_position
    """)
    for col in cur.fetchall():
        print(f"   {col[0]}: {col[1]}")
    
    print("\n📋 crawl_history 테이블 스키마:")
    cur.execute("""
        SELECT column_name, data_type 
        FROM information_schema.columns 
        WHERE table_name = 'crawl_history'
        ORDER BY ordinal_position
    """)
    for col in cur.fetchall():
        print(f"   {col[0]}: {col[1]}")
    
    cur.close()
    conn.close()
    
except Exception as e:
    print(f"❌ 오류: {e}")
