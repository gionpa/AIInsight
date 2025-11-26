#!/usr/bin/env python3
import psycopg2
import sys

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

def migrate_data():
    print("🔄 로컬 PostgreSQL → Railway PostgreSQL 데이터 마이그레이션 시작...")
    
    try:
        local_conn = psycopg2.connect(**LOCAL_DB)
        local_cur = local_conn.cursor()
        
        railway_conn = psycopg2.connect(**RAILWAY_DB)
        railway_cur = railway_conn.cursor()
        
        # Railway에 있는 target_id 확인
        railway_cur.execute("SELECT id FROM crawl_target")
        valid_target_ids = {row[0] for row in railway_cur.fetchall()}
        print(f"📌 Railway DB 유효한 타겟 ID: {sorted(valid_target_ids)}")
        
        # 1. news_article 마이그레이션 (ON CONFLICT 제거, 수동 중복 체크)
        print("\n📰 뉴스 기사 데이터 마이그레이션...")
        local_cur.execute("SELECT COUNT(*) FROM news_article")
        print(f"   로컬 DB: {local_cur.fetchone()[0]}개 기사")
        
        # Railway에 이미 있는 URL 확인
        railway_cur.execute("SELECT original_url FROM news_article")
        existing_urls = {row[0] for row in railway_cur.fetchall()}
        print(f"   Railway DB 기존 URL: {len(existing_urls)}개")
        
        local_cur.execute("""
            SELECT id, target_id, original_url, title, title_ko, content, 
                   summary, author, published_at, relevance_score, 
                   category, importance, is_new, is_summarized, 
                   thumbnail_url, content_hash, crawled_at, updated_at
            FROM news_article
            WHERE target_id = ANY(%s)
            ORDER BY id
        """, (list(valid_target_ids),))
        
        migrated = 0
        skipped = 0
        for article in local_cur.fetchall():
            url = article[2]
            if url in existing_urls:
                skipped += 1
                continue
            
            try:
                railway_cur.execute("""
                    INSERT INTO news_article 
                    (id, target_id, original_url, title, title_ko, content, 
                     summary, author, published_at, relevance_score, 
                     category, importance, is_new, is_summarized, 
                     thumbnail_url, content_hash, crawled_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, article)
                migrated += 1
                railway_conn.commit()
            except Exception as e:
                railway_conn.rollback()
                print(f"   ⚠️  기사 ID {article[0]} 실패: {e}")
                skipped += 1
        
        print(f"   ✅ {migrated}개 마이그레이션, {skipped}개 스킵")
        
        # 2. crawl_history 마이그레이션 (유효한 target_id만)
        print("\n📊 크롤링 히스토리 마이그레이션...")
        local_cur.execute("SELECT COUNT(*) FROM crawl_history")
        print(f"   로컬 DB: {local_cur.fetchone()[0]}개 히스토리")
        
        local_cur.execute("""
            SELECT id, target_id, status, articles_found, articles_new, 
                   duration_ms, error_message, executed_at
            FROM crawl_history
            WHERE target_id = ANY(%s)
            ORDER BY id
        """, (list(valid_target_ids),))
        
        migrated = 0
        skipped = 0
        for history in local_cur.fetchall():
            try:
                railway_cur.execute("""
                    INSERT INTO crawl_history 
                    (id, target_id, status, articles_found, articles_new, 
                     duration_ms, error_message, executed_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """, history)
                migrated += 1
                railway_conn.commit()
            except psycopg2.errors.UniqueViolation:
                railway_conn.rollback()
                skipped += 1
            except Exception as e:
                railway_conn.rollback()
                print(f"   ⚠️  히스토리 ID {history[0]} 실패: {e}")
                skipped += 1
        
        print(f"   ✅ {migrated}개 마이그레이션, {skipped}개 스킵")
        
        # 3. 최종 확인
        print("\n📊 Railway PostgreSQL 최종 상태:")
        railway_cur.execute("SELECT COUNT(*) FROM crawl_target")
        print(f"   크롤링 타겟: {railway_cur.fetchone()[0]}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM news_article")
        article_count = railway_cur.fetchone()[0]
        print(f"   뉴스 기사: {article_count}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM crawl_history")
        print(f"   크롤링 히스토리: {railway_cur.fetchone()[0]}개")
        
        if article_count > 0:
            print("\n📋 최신 기사 샘플:")
            railway_cur.execute("""
                SELECT id, title_ko, title, crawled_at 
                FROM news_article 
                ORDER BY crawled_at DESC 
                LIMIT 5
            """)
            for row in railway_cur.fetchall():
                title = row[1] or row[2]
                print(f"   [{row[0]}] {title[:50]}... ({row[3]})")
        
        local_cur.close()
        local_conn.close()
        railway_cur.close()
        railway_conn.close()
        
        print("\n🎉 마이그레이션 완료!")
        
    except Exception as e:
        print(f"❌ 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    migrate_data()
