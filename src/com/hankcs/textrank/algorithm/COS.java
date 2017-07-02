package com.hankcs.textrank.algorithm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * COS 算法
 * Created by MissingNo on 2017/5/14.
 */
public class COS implements IAlgorithm {

    /**
     * 拆分为[句子[单词]]形式的文档
     */
    private List<List<String>> docs;

    public COS(List<List<String>> docs){
        this.docs = docs;
    }

    /**
     * 计算两个字符串(英文字符)的相似度，简单的余弦计算，未添权重
     */
    private double getSimilarDegree(List<String> str1, List<String> str2)
    {
        //创建向量空间模型，使用map实现，主键为词项，值为长度为2的数组，存放着对应词项在字符串中的出现次数
        Map<String, int[]> vectorSpace = new HashMap<>();
        int[] itemCountArray;//为了避免频繁产生局部变量，所以将itemCountArray声明在此

        for(int i=0; i<str1.size(); ++i)
        {
            if(vectorSpace.containsKey(str1.get(i)))
                ++(vectorSpace.get(str1.get(i))[0]);
            else
            {
                itemCountArray = new int[2];
                itemCountArray[0] = 1;
                itemCountArray[1] = 0;
                vectorSpace.put(str1.get(i), itemCountArray);
            }
        }

        for(int i=0; i<str2.size(); ++i)
        {
            if(vectorSpace.containsKey(str2.get(i)))
                ++(vectorSpace.get(str2.get(i))[1]);
            else
            {
                itemCountArray = new int[2];
                itemCountArray[0] = 0;
                itemCountArray[1] = 1;
                vectorSpace.put(str2.get(i), itemCountArray);
            }
        }

        //计算相似度
        double vector1Modulo = 0.00;//向量1的模
        double vector2Modulo = 0.00;//向量2的模
        double vectorProduct = 0.00; //向量积
        Iterator iter = vectorSpace.entrySet().iterator();

        while(iter.hasNext())
        {
            Map.Entry entry = (Map.Entry)iter.next();
            itemCountArray = (int[])entry.getValue();

            vector1Modulo += itemCountArray[0]*itemCountArray[0];
            vector2Modulo += itemCountArray[1]*itemCountArray[1];

            vectorProduct += itemCountArray[0]*itemCountArray[1];
        }

        vector1Modulo = Math.sqrt(vector1Modulo);
        vector2Modulo = Math.sqrt(vector2Modulo);

        //返回相似度
        return (vectorProduct/(vector1Modulo*vector2Modulo));
    }

    @Override
    public double[] simAll(int sentenceID) {
        double[] scores = new double[docs.size()];
        for (int i = 0; i < docs.size(); ++i)
        {
            scores[i] = getSimilarDegree(docs.get(sentenceID), docs.get(i));
        }
        return scores;
    }

}
