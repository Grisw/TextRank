package com.hankcs.textrank.algorithm;

import java.util.*;

/**
 * 搜索相关性评分算法
 * @author hankcs
 */
public class BM25 implements IAlgorithm
{
    /**
     * 文档句子的个数
     */
    private int D;

    /**
     * 文档句子的平均长度
     */
    private double avgdl;

    /**
     * 拆分为[句子[单词]]形式的文档
     */
    private List<List<String>> docs;

    /**
     * 文档中每个句子中的每个词与词频
     */
    private Map<String, Integer>[] f;

    /**
     * 文档中全部词语与出现在几个句子中
     */
    private Map<String, Integer> df;

    /**
     * IDF
     */
    private Map<String, Double> idf;

    /**
     * 调节因子
     */
    private final static float k1 = 1.5f;

    /**
     * 调节因子
     */
    private final static float k2 = 1.5f;

    /**
     * 调节因子
     */
    private final static float b = 0.2f;

    public BM25(List<List<String>> docs)
    {
        this.docs = docs;
        D = docs.size();
        for (List<String> sentence : docs)
        {
            avgdl += sentence.size();
        }
        avgdl /= D;
        f = new Map[D];
        df = new TreeMap<>();
        idf = new TreeMap<>();
        init();
    }

    /**
     * 在构造时初始化自己的所有参数
     */
    private void init()
    {
        int index = 0;
        for (List<String> sentence : docs)
        {
            Map<String, Integer> tf = new TreeMap<>();
            for (String word : sentence)
            {
                Integer freq = tf.get(word);
                freq = (freq == null ? 0 : freq) + 1;
                tf.put(word, freq);
            }
            f[index] = tf;
            for (Map.Entry<String, Integer> entry : tf.entrySet())
            {
                String word = entry.getKey();
                Integer freq = df.get(word);
                freq = (freq == null ? 0 : freq) + 1;
                df.put(word, freq);
            }
            ++index;
        }
        for (Map.Entry<String, Integer> entry : df.entrySet())
        {
            String word = entry.getKey();
            Integer freq = entry.getValue();
            //idf.put(word, Math.log(D - freq + 0.5) - Math.log(freq + 0.5));
            idf.put(word, (double)freq / D);
        }
    }

    private double sim(int sentenceID, int index)
    {
        double score = 0;
        Set<String> sentence = new HashSet<>(docs.get(sentenceID));
        for (String word : sentence)
        {
            if (!f[index].containsKey(word)||!f[sentenceID].containsKey(word)) continue;
            int d = docs.get(index).size();
            int wf = f[index].get(word);
            int qf = f[sentenceID].get(word);
            score += (idf.get(word) * wf * (k1 + 1)
                    / (wf + k1 * (1 - b + b * d/ avgdl)))*((qf * (k2 + 1))/(qf + k2));
        }

        return score;
    }

    @Override
    public double[] simAll(int sentenceID)
    {
        double[] scores = new double[D];
        for (int i = 0; i < D; ++i)
        {
            scores[i] = sim(sentenceID, i);
        }
        return scores;
    }
}
