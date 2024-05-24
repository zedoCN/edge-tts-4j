package top.zedo.edgetts;

import top.zedo.edgetts.info.TTSMetadataInfo;
import top.zedo.edgetts.info.TTSResponseInfo;

public interface ITTSListener {
    /**
     * 获得音频数据
     *
     * @param requestId   请求ID 如 “443f5d3dfcf64a95a12bd85df2b255a4”
     * @param streamId    如 “46371DE2E7F04D4992877D3AC0119BB9”
     * @param contentType 数据类型 如 “audio/webm”
     * @param codec       解码器  如 “opus”
     * @param audioData   音频数据
     */
    default void onGetAudioData(String requestId, String streamId, String contentType, String codec, byte[] audioData) {

    }

    /**
     * 音频数据结束
     *
     * @param requestId 请求ID 如 “443f5d3dfcf64a95a12bd85df2b255a4”
     * @param streamId  如 “46371DE2E7F04D4992877D3AC0119BB9”
     */
    default void onGetAudioDataEnd(String requestId, String streamId) {

    }

    /**
     * 连接成功
     */
    default void onConnect() {

    }

    /**
     * 断开连接
     */
    default void onDisconnect() {

    }

    /**
     * 开始转换
     *
     * @param requestId 请求ID
     * @param info      一些信息
     */
    default void onGetTurnStart(String requestId, TTSResponseInfo info) {

    }

    /**
     * 转换结束
     *
     * @param requestId 请求ID
     */
    default void onGetTurnEnd(String requestId) {

    }

    /**
     * 获得响应
     *
     * @param requestId 请求ID
     * @param info      一些信息
     */
    default void onGetResponse(String requestId, TTSResponseInfo info) {

    }

    /**
     * 获得音频元数据
     *
     * @param requestId 请求ID
     * @param info      一些信息
     */
    default void onGetAudioMetadata(String requestId, TTSMetadataInfo info) {

    }
}
