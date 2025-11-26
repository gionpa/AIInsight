#!/usr/bin/env python3
"""
Railway PostgreSQL Database Setup Script
"""

import psycopg2
from psycopg2 import sql

# Database connection parameters
DATABASE_URL = "postgresql://postgres:yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg@yamanote.proxy.rlwy.net:51273/railway"

def main():
    print("🚀 Connecting to Railway PostgreSQL...")

    try:
        # Connect to the database
        conn = psycopg2.connect(DATABASE_URL)
        conn.autocommit = True
        cursor = conn.cursor()

        print("✅ Connected successfully!")

        # Read SQL file
        with open('/Users/user/claude_projects/AIInsight/railway-schema.sql', 'r') as f:
            sql_script = f.read()

        print("\n📋 Executing schema creation...")

        # Execute the SQL script
        cursor.execute(sql_script)

        # Get the result
        result = cursor.fetchone()
        print(f"\n✅ {result[0]}")
        print(f"📊 Crawl targets inserted: {result[1]}")

        # Verify tables
        cursor.execute("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name
        """)

        tables = cursor.fetchall()
        print(f"\n📦 Created tables:")
        for table in tables:
            cursor.execute(f"SELECT COUNT(*) FROM {table[0]}")
            count = cursor.fetchone()[0]
            print(f"   - {table[0]}: {count} rows")

        cursor.close()
        conn.close()

        print("\n🎉 Database setup completed successfully!")

    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

    return 0

if __name__ == "__main__":
    exit(main())
