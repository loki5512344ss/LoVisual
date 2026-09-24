package dev.loki.lovisual.render.engine.rhi.uniform.stats;
import dev.loki.lovisual.render.engine.rhi.uniform.stats.UniformAllocatorStatsSnapshot;

public final class UniformAllocatorStats {
    private long frameId;
    private long writes;
    private long uploadedBytes;
    private long streamCount;
    private long activeStreams;
    private long ringCapacityBytes;
    private long ringCursorBytes;
    private long ringRotations;
    private long ringGrows;
    private long blockingBufferRequests;
    private long staleReadMisses;

    public void beginFrame(long frameId) {
        this.frameId = frameId;
        writes = 0L;
        uploadedBytes = 0L;
        activeStreams = 0L;
        ringCursorBytes = 0L;
        ringRotations = 0L;
        ringGrows = 0L;
        blockingBufferRequests = 0L;
        staleReadMisses = 0L;
    }

    public void write(long bytes) {
        writes++;
        uploadedBytes += bytes;
    }

    public void activeStreams(long count) {
        activeStreams = count;
    }

    public void streamCount(long count) {
        streamCount = count;
    }

    public void ringCapacityBytes(long bytes) {
        ringCapacityBytes = bytes;
    }

    public void ringCursorBytes(long bytes) {
        ringCursorBytes = bytes;
    }

    public void ringRotation() {
        ringRotations++;
    }

    public void ringGrow() {
        ringGrows++;
    }

    public void blockingBufferRequest() {
        blockingBufferRequests++;
    }

    public void staleReadMiss() {
        staleReadMisses++;
    }

    public long writes() {
        return writes;
    }

    public long uploadedBytes() {
        return uploadedBytes;
    }

    public long streamCount() {
        return streamCount;
    }

    public long activeStreams() {
        return activeStreams;
    }

    public long ringCapacityBytes() {
        return ringCapacityBytes;
    }

    public long ringCursorBytes() {
        return ringCursorBytes;
    }

    public long ringRotations() {
        return ringRotations;
    }

    public long ringGrows() {
        return ringGrows;
    }

    public long blockingBufferRequests() {
        return blockingBufferRequests;
    }

    public long staleReadMisses() {
        return staleReadMisses;
    }

    public UniformAllocatorStatsSnapshot snapshot() {
        return new UniformAllocatorStatsSnapshot(frameId, writes, uploadedBytes, streamCount, activeStreams,
                ringCapacityBytes, ringCursorBytes, ringRotations, ringGrows, blockingBufferRequests, staleReadMisses);
    }
}
