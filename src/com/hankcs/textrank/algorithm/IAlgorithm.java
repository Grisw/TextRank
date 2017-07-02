package com.hankcs.textrank.algorithm;

/**
 * 相关性评分算法接口
 * Created by MissingNo on 2017/5/12.
 */
public interface IAlgorithm {
    double[] simAll(int sentenceID);
}
