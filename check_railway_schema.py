#!/usr/bin/env python3
import psycopg2

RAILWAY_DB = {
    'host': 'yamanote.proxy.rlwy.net',
    'port': 51273,
    'database': 'railway',
    'user': 'postgres',
    'password': 'yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg'
}

try:
    conn = psycopg2.connect(**RAILWAY_DB)
    cur = conn.cursor()
    
    print("📋 Railway news_article 테이블 스키마:")
    cur.execute("""
        SELECT column_name, data_type 
        FROM information_schema.columns 
        WHERE table_name = 'news_article'
        ORDER BY ordinal_position
    """)
    for col in cur.fetchall():
        print(f"   {col[0]}: {col[1]}")
    
    print("\n📋 Railway crawl_history 테이블 스키마:")
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
