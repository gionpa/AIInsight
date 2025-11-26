#!/usr/bin/env python3
import psycopg2
import sys

# 로컬 Podman PostgreSQL 연결 정보
LOCAL_DB = {
    'host': 'localhost',
    'port': 5432,
    'database': 'aiinsight',
    'user': 'aiinsight',
    'password': 'aiinsight123'
}

# Railway PostgreSQL 연결 정보
RAILWAY_DB = {
    'host': 'yamanote.proxy.rlwy.net',
    'port': 51273,
    'database': 'railway',
    'user': 'postgres',
    'password': 'yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg'
}

def migrate_data():
    print("🔄 로컬 PostgreSQL → Railway PostgreSQL 데이터 마이그레이션 시작...")
    
    try:
        # 로컬 DB 연결
        print(f"📡 로컬 DB 연결 중... ({LOCAL_DB['host']}:{LOCAL_DB['port']})")
        local_conn = psycopg2.connect(**LOCAL_DB)
        local_cur = local_conn.cursor()
        
        # Railway DB 연결
        print(f"📡 Railway DB 연결 중... ({RAILWAY_DB['host']}:{RAILWAY_DB['port']})")
        railway_conn = psycopg2.connect(**RAILWAY_DB)
        railway_cur = railway_conn.cursor()
        
        # 1. news_article 데이터 확인 및 마이그레이션
        print("\n📰 뉴스 기사 데이터 확인 중...")
        local_cur.execute("SELECT COUNT(*) FROM news_article")
        local_article_count = local_cur.fetchone()[0]
        print(f"   로컬 DB: {local_article_count}개 기사")
        
        if local_article_count > 0:
            print(f"   📤 {local_article_count}개 기사 마이그레이션 중...")
            
            # 기사 데이터 추출
            local_cur.execute("""
                SELECT id, url, title, title_ko, content, author, published_at, 
                       crawled_at, ai_summary, category, importance, relevance_score, 
                       crawl_target_id, analyzed
                FROM news_article
                ORDER BY id
            """)
            articles = local_cur.fetchall()
            
            # Railway DB에 삽입
            migrated = 0
            for article in articles:
                try:
                    railway_cur.execute("""
                        INSERT INTO news_article 
                        (id, url, title, title_ko, content, author, published_at, 
                         crawled_at, ai_summary, category, importance, relevance_score, 
                         crawl_target_id, analyzed)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (url) DO NOTHING
                    """, article)
                    migrated += 1
                except Exception as e:
                    print(f"   ⚠️  기사 ID {article[0]} 삽입 실패: {e}")
            
            railway_conn.commit()
            print(f"   ✅ {migrated}개 기사 마이그레이션 완료")
        else:
            print("   ℹ️  로컬 DB에 기사 데이터가 없습니다.")
        
        # 2. crawl_history 데이터 확인 및 마이그레이션
        print("\n📊 크롤링 히스토리 확인 중...")
        local_cur.execute("SELECT COUNT(*) FROM crawl_history")
        local_history_count = local_cur.fetchone()[0]
        print(f"   로컬 DB: {local_history_count}개 히스토리")
        
        if local_history_count > 0:
            print(f"   📤 {local_history_count}개 히스토리 마이그레이션 중...")
            
            # 히스토리 데이터 추출
            local_cur.execute("""
                SELECT id, crawl_target_id, started_at, finished_at, 
                       status, articles_found, articles_saved, error_message
                FROM crawl_history
                ORDER BY id
            """)
            histories = local_cur.fetchall()
            
            # Railway DB에 삽입
            migrated = 0
            for history in histories:
                try:
                    railway_cur.execute("""
                        INSERT INTO crawl_history 
                        (id, crawl_target_id, started_at, finished_at, 
                         status, articles_found, articles_saved, error_message)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (id) DO NOTHING
                    """, history)
                    migrated += 1
                except Exception as e:
                    print(f"   ⚠️  히스토리 ID {history[0]} 삽입 실패: {e}")
            
            railway_conn.commit()
            print(f"   ✅ {migrated}개 히스토리 마이그레이션 완료")
        else:
            print("   ℹ️  로컬 DB에 히스토리 데이터가 없습니다.")
        
        # 3. 최종 확인
        print("\n✅ 마이그레이션 완료! Railway PostgreSQL 최종 상태:")
        railway_cur.execute("SELECT COUNT(*) FROM crawl_target")
        print(f"   크롤링 타겟: {railway_cur.fetchone()[0]}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM news_article")
        print(f"   뉴스 기사: {railway_cur.fetchone()[0]}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM crawl_history")
        print(f"   크롤링 히스토리: {railway_cur.fetchone()[0]}개")
        
        local_cur.close()
        local_conn.close()
        railway_cur.close()
        railway_conn.close()
        
        print("\n🎉 데이터 마이그레이션 성공!")
        
    except psycopg2.OperationalError as e:
        print(f"❌ 데이터베이스 연결 실패: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ 마이그레이션 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    migrate_data()
