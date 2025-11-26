#!/usr/bin/env python3
"""
Railway Redis Connection Test Script
"""

import redis

# Redis connection URL
REDIS_URL = "redis://default:EHndhkZGTobgKYkUrgWIhevIpYLQivNy@interchange.proxy.rlwy.net:19189"

def main():
    print("🔄 Connecting to Railway Redis...")

    try:
        # Connect to Redis
        r = redis.from_url(REDIS_URL, decode_responses=True)

        # Test connection
        r.ping()
        print("✅ Connected successfully!")

        # Test SET operation
        r.set("test_key", "Hello from AIInsight!")
        print("✅ SET operation successful")

        # Test GET operation
        value = r.get("test_key")
        print(f"✅ GET operation successful: {value}")

        # Test DELETE operation
        r.delete("test_key")
        print("✅ DELETE operation successful")

        # Get Redis info
        info = r.info("server")
        print(f"\n📊 Redis Server Info:")
        print(f"   - Version: {info.get('redis_version')}")
        print(f"   - Mode: {info.get('redis_mode')}")
        print(f"   - OS: {info.get('os')}")

        # Get memory info
        memory_info = r.info("memory")
        used_memory = memory_info.get('used_memory_human')
        print(f"   - Used Memory: {used_memory}")

        print("\n🎉 All Redis operations completed successfully!")

    except redis.ConnectionError as e:
        print(f"❌ Connection Error: {e}")
        return 1
    except redis.AuthenticationError as e:
        print(f"❌ Authentication Error: {e}")
        return 1
    except Exception as e:
        print(f"❌ Error: {e}")
        return 1

    return 0

if __name__ == "__main__":
    exit(main())
