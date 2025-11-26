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

def clean_and_migrate():
    print("🔄 Railway PostgreSQL 데이터 정리 및 재마이그레이션 시작...")
    
    try:
        local_conn = psycopg2.connect(**LOCAL_DB)
        local_cur = local_conn.cursor()
        
        railway_conn = psycopg2.connect(**RAILWAY_DB)
        railway_cur = railway_conn.cursor()
        
        # 1. Railway DB 기존 데이터 삭제
        print("\n🗑️  Railway DB 기존 데이터 삭제 중...")
        
        # Foreign key 제약 때문에 순서대로 삭제
        railway_cur.execute("DELETE FROM news_article")
        deleted_articles = railway_cur.rowcount
        print(f"   - news_article: {deleted_articles}개 삭제")
        
        railway_cur.execute("DELETE FROM crawl_history")
        deleted_history = railway_cur.rowcount
        print(f"   - crawl_history: {deleted_history}개 삭제")
        
        railway_cur.execute("DELETE FROM crawl_target")
        deleted_targets = railway_cur.rowcount
        print(f"   - crawl_target: {deleted_targets}개 삭제")
        
        railway_conn.commit()
        print("   ✅ 기존 데이터 삭제 완료")
        
        # 2. crawl_target 마이그레이션
        print("\n📌 크롤링 타겟 마이그레이션...")
        local_cur.execute("SELECT COUNT(*) FROM crawl_target")
        local_target_count = local_cur.fetchone()[0]
        print(f"   로컬 DB: {local_target_count}개 타겟")
        
        local_cur.execute("""
            SELECT id, name, url, crawl_type, selector, is_active, 
                   schedule_cron, last_crawled_at, created_at, updated_at
            FROM crawl_target
            ORDER BY id
        """)
        
        migrated = 0
        for target in local_cur.fetchall():
            try:
                railway_cur.execute("""
                    INSERT INTO crawl_target 
                    (id, name, url, crawl_type, selector, is_active, 
                     schedule_cron, last_crawled_at, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, target)
                migrated += 1
            except Exception as e:
                print(f"   ⚠️  타겟 ID {target[0]} 실패: {e}")
        
        railway_conn.commit()
        print(f"   ✅ {migrated}개 타겟 마이그레이션 완료")
        
        # 3. news_article 마이그레이션
        print("\n📰 뉴스 기사 마이그레이션...")
        local_cur.execute("SELECT COUNT(*) FROM news_article")
        local_article_count = local_cur.fetchone()[0]
        print(f"   로컬 DB: {local_article_count}개 기사")
        
        local_cur.execute("""
            SELECT id, target_id, original_url, title, title_ko, content, 
                   summary, author, published_at, relevance_score, 
                   category, importance, is_new, is_summarized, 
                   thumbnail_url, content_hash, crawled_at, updated_at
            FROM news_article
            ORDER BY id
        """)
        
        migrated = 0
        for article in local_cur.fetchall():
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
            except Exception as e:
                print(f"   ⚠️  기사 ID {article[0]} 실패: {e}")
        
        railway_conn.commit()
        print(f"   ✅ {migrated}개 기사 마이그레이션 완료")
        
        # 4. crawl_history 마이그레이션
        print("\n📊 크롤링 히스토리 마이그레이션...")
        local_cur.execute("SELECT COUNT(*) FROM crawl_history")
        local_history_count = local_cur.fetchone()[0]
        print(f"   로컬 DB: {local_history_count}개 히스토리")
        
        local_cur.execute("""
            SELECT id, target_id, status, articles_found, articles_new, 
                   duration_ms, error_message, executed_at
            FROM crawl_history
            ORDER BY id
        """)
        
        migrated = 0
        for history in local_cur.fetchall():
            try:
                railway_cur.execute("""
                    INSERT INTO crawl_history 
                    (id, target_id, status, articles_found, articles_new, 
                     duration_ms, error_message, executed_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """, history)
                migrated += 1
            except Exception as e:
                print(f"   ⚠️  히스토리 ID {history[0]} 실패: {e}")
        
        railway_conn.commit()
        print(f"   ✅ {migrated}개 히스토리 마이그레이션 완료")
        
        # 5. 최종 확인
        print("\n" + "="*60)
        print("📊 Railway PostgreSQL 최종 데이터 상태")
        print("="*60)
        
        railway_cur.execute("SELECT COUNT(*) FROM crawl_target")
        target_count = railway_cur.fetchone()[0]
        print(f"   크롤링 타겟: {target_count}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM news_article")
        article_count = railway_cur.fetchone()[0]
        print(f"   뉴스 기사: {article_count}개")
        
        railway_cur.execute("SELECT COUNT(*) FROM crawl_history")
        history_count = railway_cur.fetchone()[0]
        print(f"   크롤링 히스토리: {history_count}개")
        
        if article_count > 0:
            print("\n📋 카테고리별 기사 분포:")
            railway_cur.execute("""
                SELECT category, COUNT(*) as cnt
                FROM news_article
                WHERE category IS NOT NULL
                GROUP BY category
                ORDER BY cnt DESC
            """)
            for row in railway_cur.fetchall():
                print(f"   - {row[0]}: {row[1]}개")
            
            print("\n📋 최신 기사 5개:")
            railway_cur.execute("""
                SELECT id, title_ko, title, crawled_at, category
                FROM news_article 
                ORDER BY crawled_at DESC 
                LIMIT 5
            """)
            for row in railway_cur.fetchall():
                title = row[1] or row[2]
                category = row[4] or "기타"
                print(f"   [{row[0]}] [{category}] {title[:50]}...")
        
        local_cur.close()
        local_conn.close()
        railway_cur.close()
        railway_conn.close()
        
        print("\n" + "="*60)
        print("🎉 전체 데이터 마이그레이션 완료!")
        print("="*60)
        
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    clean_and_migrate()
