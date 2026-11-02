package it.ttsserverproxy;

import android.media.MediaCodec;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Decodifica audio compresso (es. MP3 restituito dal server) in PCM 16-bit. */
final class AudioDecoder {
    private AudioDecoder() {}

    static final class Pcm {
        final int rate, channels;
        final byte[] data;
        Pcm(int rate, int channels, byte[] data) { this.rate = rate; this.channels = channels; this.data = data; }
    }

    static Pcm decode(byte[] audio) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(new ByteSource(audio));
            if (extractor.getTrackCount() < 1) throw new IOException("Nessuna traccia audio nella risposta");
            extractor.selectTrack(0);
            MediaFormat inFormat = extractor.getTrackFormat(0);
            String mime = inFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null) throw new IOException("MIME traccia audio sconosciuto");
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(inFormat, null, null, 0);
            codec.start();

            int rate = inFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 24000;
            int channels = inFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputEos = false, outputEos = false;
            int guard = 0;
            while (!outputEos && guard++ < 100000) {
                if (!inputEos) {
                    int inIdx = codec.dequeueInputBuffer(10_000);
                    if (inIdx >= 0) {
                        ByteBuffer in = codec.getInputBuffer(inIdx);
                        int size = in == null ? -1 : extractor.readSampleData(in, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEos = true;
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int outIdx = codec.dequeueOutputBuffer(info, 10_000);
                while (outIdx >= 0 || outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat of = codec.getOutputFormat();
                        if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    } else {
                        ByteBuffer ob = codec.getOutputBuffer(outIdx);
                        if (ob != null && info.size > 0) {
                            byte[] chunk = new byte[info.size];
                            ob.position(info.offset);
                            ob.get(chunk);
                            out.write(chunk, 0, chunk.length);
                        }
                        codec.releaseOutputBuffer(outIdx, false);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) { outputEos = true; break; }
                    }
                    outIdx = codec.dequeueOutputBuffer(info, 0);
                }
            }
            if (out.size() == 0) throw new IOException("Decodifica audio senza output PCM");
            return new Pcm(rate, channels, out.toByteArray());
        } finally {
            if (codec != null) { try { codec.stop(); } catch (Exception ignored) {} codec.release(); }
            extractor.release();
        }
    }

    private static final class ByteSource extends MediaDataSource {
        private final byte[] d;
        ByteSource(byte[] d) { this.d = d; }
        @Override public int readAt(long position, byte[] buffer, int offset, int size) {
            if (position >= d.length) return -1;
            int n = (int) Math.min(size, d.length - position);
            System.arraycopy(d, (int) position, buffer, offset, n);
            return n;
        }
        @Override public long getSize() { return d.length; }
        @Override public void close() {}
    }
}
