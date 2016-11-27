package org.mongodb.morphia.session;

/**
 * Keeps track of various session statistics such as cache hits, queries, updated records, and so on so you can analyze
 * and optimize your query optimizations.
 */
public class SessionStats
{
    private int cacheHits;
    private int totalReads;
    private int totalWrites;
    private int totalDeletes;

    public SessionStats()
    {
        cacheHits = 0;
        totalReads = 0;
        totalWrites = 0;
        totalDeletes = 0;
    }

    public int getCacheHits() { return cacheHits; }
    public void setCacheHits(int cacheHits) { this.cacheHits = cacheHits; }

    public int getTotalReads() { return totalReads; }
    public void setTotalReads(int totalReads) { this.totalReads = totalReads; }
    
    public int getTotalWrites() { return totalWrites; }
    public void setTotalWrites(int totalWrites) { this.totalWrites = totalWrites; }

    public int getTotalDeletes() { return totalDeletes; }
    public void setTotalDeletes(int totalDeletes) { this.totalDeletes = totalDeletes; }

    /**
     * Increments the total cache hits by 1
     * @return this (for chaining)
     */
    public SessionStats cacheHit()
    {
        cacheHits++;
        return this;
    }

    /**
     * Increments the total read queries executed by 1
     * @return this (for chaining)
     */
    public SessionStats read()
    {
        totalReads++;
        return this;
    }

    /**
     * Increments the total number of objects created/modified by 1
     * @return this (for chaining)
     */
    public SessionStats write()
    {
        totalWrites++;
        return this;
    }

    /**
     * Increments the total number of objects deleted
     * @return this (for chaining)
     */
    public SessionStats delete()
    {
        totalDeletes++;
        return this;
    }
}
