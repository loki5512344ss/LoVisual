package dev.loki.lovisual.render.engine.rhi.uniform.stats;

public record UniformAllocatorStatsSnapshot(long frameId,
                                            long writes,
                                            long uploadedBytes,
                                            long streamCount,
                                            long activeStreams,
                                            long ringCapacityBytes,
                                            long ringCursorBytes,
                                            long ringRotations,
                                            long ringGrows,
                                            long blockingBufferRequests,
                                            long staleReadMisses) {
}
