#!/usr/bin/env python3
import psycopg2

LOCAL_DB = {
    'host': 'localhost',
    'port': 5432,
    'database': 'aiinsight',
    'user': 'aiinsight',
    'password': 'aiinsight123'
}

RAILWAY_DB = {
    'host': 'yamanote.proxy.rlwy.net',
    'port': 51273,
    'database': 'railway',
    'user': 'postgres',
    'password': 'yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg'
}

try:
    # 로컬 DB
    local_conn = psycopg2.connect(**LOCAL_DB)
    local_cur = local_conn.cursor()
    
    print("📋 로컬 crawl_target 스키마:")
    local_cur.execute("""
        SELECT column_name, data_type 
        FROM information_schema.columns 
        WHERE table_name = 'crawl_target'
        ORDER BY ordinal_position
    """)
    for col in local_cur.fetchall():
        print(f"   {col[0]}: {col[1]}")
    
    # Railway DB
    railway_conn = psycopg2.connect(**RAILWAY_DB)
    railway_cur = railway_conn.cursor()
    
    print("\n📋 Railway crawl_target 스키마:")
    railway_cur.execute("""
        SELECT column_name, data_type 
        FROM information_schema.columns 
        WHERE table_name = 'crawl_target'
        ORDER BY ordinal_position
    """)
    for col in railway_cur.fetchall():
        print(f"   {col[0]}: {col[1]}")
    
    local_cur.close()
    local_conn.close()
    railway_cur.close()
    railway_conn.close()
    
except Exception as e:
    print(f"❌ 오류: {e}")
