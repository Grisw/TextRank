package com.hankcs.textrank;

import com.hankcs.textrank.algorithm.BM25;
import com.hankcs.textrank.algorithm.COS;
import com.hankcs.textrank.algorithm.IAlgorithm;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import javafx.util.Pair;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * TextRank 自动摘要
 * @author hankcs
 */
public class TextRankSummary
{
    /**
     * 阻尼系数（ＤａｍｐｉｎｇＦａｃｔｏｒ），一般取值为0.85
     */
    private static final double d = 0.85f;
    /**
     * 最大迭代次数
     */
    private static final int max_iter = 200;
    private static final double min_diff = 0.001f;
    /**
     * 文档句子的个数
     */
    private int D;
    /**
     * 拆分为[句子[单词]]形式的文档
     */
    private List<List<String>> docs;
    /**
     * 排序后的最终结果 score <-> index
     */
    private TreeMap<Double, Integer> top;

    /**
     * 句子和其他句子的相关程度
     */
    private double[][] weight;
    /**
     * 该句子和其他句子相关程度之和
     */
    private double[] weight_sum;
    /**
     * 迭代之后收敛的权重
     */
    private double[] vertex;

    /**
     * 相似度算法
     */
    private IAlgorithm algorithm;

    private static String[] stopwords = {"able","about","above","according","accordingly","across","actually","after","afterwards","again","against","ain't","all","allow","allows","almost","alone","along","already","also","although","always","am","among","amongst","an","and","another","any","anybody","anyhow","anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate","appropriate","are","aren't","around","as","a's","aside","ask","asking","associated","at","available","away","awfully","be","became","because","become","becomes","becoming","been","before","beforehand","behind","being","believe","below","beside","besides","best","better","between","beyond","both","brief","but","by","came","can","cannot","cant","can't","cause","causes","certain","certainly","changes","clearly","c'mon","co","com","come","comes","concerning","consequently","consider","considering","contain","containing","contains","corresponding","could","couldn't","course","c's","currently","definitely","described","despite","did","didn't","different","do","does","doesn't","doing","done","don't","down","downwards","during","each","edu","eg","eight","either","else","elsewhere","enough","entirely","especially","et","etc","even","ever","every","everybody","everyone","everything","everywhere","ex","exactly","example","except","far","few","fifth","first","five","followed","following","follows","for","former","formerly","forth","four","from","further","furthermore","get","gets","getting","given","gives","go","goes","going","gone","got","gotten","greetings","had","hadn't","happens","hardly","has","hasn't","have","haven't","having","he","hello","help","hence","her","here","hereafter","hereby","herein","here's","hereupon","hers","herself","he's","hi","him","himself","his","hither","hopefully","how","howbeit","however","i'd","ie","if","ignored","i'll","i'm","immediate","in","inasmuch","inc","indeed","indicate","indicated","indicates","inner","insofar","instead","into","inward","is","isn't","it","it'd","it'll","its","it's","itself","i've","just","keep","keeps","kept","know","known","knows","last","lately","later","latter","latterly","least","less","lest","let","let's","like","liked","likely","little","look","looking","looks","ltd","mainly","many","may","maybe","me","mean","meanwhile","merely","might","more","moreover","most","mostly","much","must","my","myself","name","namely","nd","near","nearly","necessary","need","needs","neither","never","nevertheless","new","next","nine","no","nobody","non","none","noone","nor","normally","not","nothing","novel","now","nowhere","obviously","of","off","often","oh","ok","okay","old","on","once","one","ones","only","onto","or","other","others","otherwise","ought","our","ours","ourselves","out","outside","over","overall","own","particular","particularly","per","perhaps","placed","please","plus","possible","presumably","probably","provides","que","quite","qv","rather","rd","re","really","reasonably","regarding","regardless","regards","relatively","respectively","right","said","same","saw","say","saying","says","second","secondly","see","seeing","seem","seemed","seeming","seems","seen","self","selves","sensible","sent","serious","seriously","seven","several","shall","she","should","shouldn't","since","six","so","some","somebody","somehow","someone","something","sometime","sometimes","somewhat","somewhere","soon","sorry","specified","specify","specifying","still","sub","such","sup","sure","take","taken","tell","tends","th","than","thank","thanks","thanx","that","thats","that's","the","their","theirs","them","themselves","then","thence","there","thereafter","thereby","therefore","therein","theres","there's","thereupon","these","they","they'd","they'll","they're","they've","think","third","this","thorough","thoroughly","those","though","three","through","throughout","thru","thus","to","together","too","took","toward","towards","tried","tries","truly","try","trying","t's","twice","two","un","under","unfortunately","unless","unlikely","until","unto","up","upon","us","use","used","useful","uses","using","usually","value","various","very","via","viz","vs","want","wants","was","wasn't","way","we","we'd","welcome","well","we'll","went","were","we're","weren't","we've","what","whatever","what's","when","whence","whenever","where","whereafter","whereas","whereby","wherein","where's","whereupon","wherever","whether","which","while","whither","who","whoever","whole","whom","who's","whose","why","will","willing","wish","with","within","without","wonder","won't","would","wouldn't","yes","yet","you","you'd","you'll","your","you're","yours","yourself","yourselves","you've","zero","zt","ZT","zz","ZZ"};

    private TextRankSummary(IAlgorithm algorithm, List<List<String>> docs) {
        this.docs = docs;
        this.algorithm = algorithm;
        D = docs.size();
        weight = new double[D][D];
        weight_sum = new double[D];
        vertex = new double[D];
        top = new TreeMap<>(Collections.reverseOrder());
        solve();
    }

    private void solve() {
        for (int i = 0;i<docs.size();i++) {
            double[] scores = algorithm.simAll(i);
            //System.out.println(Arrays.toString(scores));
            weight[i] = scores;
            weight_sum[i] = sum(scores) - scores[i]; // 减掉自己，自己跟自己肯定最相似
            vertex[i] = 1.0;
        }
        for (int _ = 0; _ < max_iter; ++_) {
            double[] m = new double[D];
            double max_diff = 0;
            for (int i = 0; i < D; ++i) {
                m[i] = 1 - d;
                for (int j = 0; j < D; ++j) {
                    if (j == i || weight_sum[j] == 0)
                        continue;
                    m[i] += (d * weight[j][i] / weight_sum[j] * vertex[j]);
                }
                double diff = Math.abs(m[i] - vertex[i]);
                if (diff > max_diff) {
                    max_diff = diff;
                }
            }
            vertex = m;
            if (max_diff <= min_diff)
                break;
        }
        // 我们来排个序吧
        for (int i = 0; i < D; ++i) {
            top.put(vertex[i], i);
        }
    }

    /**
     * 获取前几个关键句子
     * @param size 要几个
     * @return 关键句子的下标
     */
    private Pair<Integer,Double>[] getTopSentence(int size)
    {
        Set<Map.Entry<Double, Integer>> values = top.entrySet();
        size = Math.min(size, values.size());
        Pair<Integer,Double>[] indexArray = new Pair[size];
        Iterator<Map.Entry<Double, Integer>> it = values.iterator();
        for (int i = 0; i < size; ++i)
        {
            Map.Entry<Double, Integer> id = it.next();
            indexArray[i] = new Pair<>(id.getValue(), id.getKey());
        }
        return indexArray;
    }

    /**
     * 简单的求和
     * @param array 数组
     * @return 和
     */
    private static double sum(double[] array)
    {
        double total = 0;
        for (double v : array)
        {
            total += v;
        }
        return total;
    }

    /**
     * 词语是否是停用词
     * @param term 词语
     * @return 是否是停用词
     */
    private static boolean shouldInclude(String term)
    {
        return Arrays.binarySearch(stopwords, term, String::compareToIgnoreCase) < 0;

    }

    /**
     * 分词操作
     * @param sentence 要分词的句子
     * @return 句子的词语列表
     */
    private static List<String> cutWords(String sentence){
        List<String> words = new ArrayList<>();
        String[] a = sentence.split(" ");
        words.addAll(Arrays.asList(a));
        return words;
    }

    /**
     * 处理分句完成的文本
     * @param sentenceList 分句完成的文本
     * @param size 需要的关键句的个数
     * @return 关键句列表
     */
    private static List<Pair<String,Double>> processTopSentenceList(String algorithm, List<String> sentenceList, int size) throws NoSuchAlgorithmException {
        List<List<String>> docs = new ArrayList<>();
        for (String sentence : sentenceList)
        {
            List<String> termList = cutWords(sentence);
            List<String> wordList = new LinkedList<>();
            for (String term : termList)
            {
                if (shouldInclude(term))
                {
                    wordList.add(term);
                }
            }
            docs.add(wordList);
        }
        IAlgorithm iAlgorithm;
        if(algorithm.equalsIgnoreCase("BM25")){
            iAlgorithm = new BM25(docs);
        }else if(algorithm.equalsIgnoreCase("COS")){
            iAlgorithm = new COS(docs);
        }else{
            throw new NoSuchAlgorithmException("Algorithm \""+algorithm+"\" is not found.");
        }

        TextRankSummary textRankSummary = new TextRankSummary(iAlgorithm, docs);
        Pair<Integer, Double>[] topSentence = textRankSummary.getTopSentence(size);
        List<Pair<String,Double>> resultList = new LinkedList<>();
        for (Pair<Integer, Double> i : topSentence)
        {
            resultList.add(new Pair<>(sentenceList.get(i.getKey()),i.getValue()));
        }
        return resultList;
    }

//    /**
//     * 一句话调用接口(格式化输入)
//     * @param algorithm 评分算法
//     * @param text 目标文档（已格式化）
//     * @param size 需要的关键句的个数
//     * @return 格式化完成的关键句列表
//     * @throws NoSuchAlgorithmException 找不到algorithm指的算法
//     */
//    public static String getTopSentenceList(String algorithm, String text, int size) throws NoSuchAlgorithmException {
//        Set<String> subs = new HashSet<>();
//        List<String> sentenceList = processText(text, subs);
//        return formatResult(processTopSentenceList(algorithm, sentenceList, size), subs, sentenceList);
//    }

    /**
     * 一句话调用接口(格式化输入)
     * @param algorithm 评分算法
     * @param text 目标文档（已格式化）
     * @param size 需要的关键句的个数
     * @return 格式化完成的关键句列表
     * @throws NoSuchAlgorithmException 找不到algorithm指的算法
     */
    public static List<Pair<String,List<String>>> getTopSentenceList(String algorithm, String text, int size) throws NoSuchAlgorithmException {
        Set<String> subs = new HashSet<>();
        List<String> sentenceList = processText(text, subs);
        return formatResult(processTopSentenceList(algorithm, sentenceList, size), subs, sentenceList);
    }

    /**
     * 处理文本文件
     * @param text 目标文档
     * @return 分句完成的列表
     */
    private static List<String> processText(String text, @Nullable Set<String> subs) {
        BufferedReader reader = new BufferedReader(new StringReader(text));
        List<String> list = new ArrayList<>();
        try {
            String sub = null;
            String line;
            while((line = reader.readLine())!=null){
                line = line.trim();
                if(line.length() == 0){
                    continue;
                }
                if(line.charAt(0)=='(' && sub == null){
                    sub = line.substring(1);
                    if(subs != null)
                        subs.add(sub);
                }else if(line.charAt(0) == '[' && sub != null){
                    line = line.substring(1, line.length());
                    list.add(sub+" "+line);
                }else if(line.equals("])")){
                    sub = null;
                }else{
                    line = line.substring(0, line.length());
                    list.add(sub+" "+line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

//    /**
//     * 将关键句格式化
//     * @param keySentences 关键句列表
//     * @return 格式化字符串
//     */
//    private static String formatResult(List<Pair<String, Double>> keySentences, @NotNull Set<String> subs, @NotNull List<String> sentences){
//        StringBuilder result = new StringBuilder();
//        List<Pair<String,Double>> keySentencesCopy = new ArrayList<>(keySentences);
//        String lastSub = null;
//        int subCount = 1;
//        int sentenceCount = 1;
//        for(String sentence : sentences){
//            for(int i = 0;i<keySentencesCopy.size();i++){
//                if(keySentencesCopy.get(i).getKey().equals(sentence)){
//                    keySentencesCopy.remove(i);
//                    for(String sub : subs){
//                        if(sentence.startsWith(sub)){
//                            if(lastSub == null || !lastSub.equals(sub)){
//                                if(sub.length() < sentence.length()){
//                                    result.append(subCount++).append(". ").append(sub).append("\n[").append(sentence.substring(sub.length()+1)).append("\n");
//                                    lastSub = sub;
//                                }else{
//                                    continue;
//                                }
//                            }else{
//                                if(sub.length() < sentence.length()){
//                                    result.append(sentence.substring(sub.length()+1)).append("\n");
//                                }else{
//                                    continue;
//                                }
//                            }
//                            break;
//                        }
//                    }
//                    break;
//                }
//            }
//        }
//        if(result.length()>0){
//            result.append("])\n");
//        }
//        return result.toString();
//    }

    /**
     * 将关键句格式化
     * @param keySentences 关键句列表
     * @return 格式化字符串
     */
    private static List<Pair<String,List<String>>> formatResult(List<Pair<String, Double>> keySentences, @NotNull Set<String> subs, @NotNull List<String> sentences){
        List<Pair<String,List<String>>> result = new ArrayList<>();
        List<Pair<String,Double>> keySentencesCopy = new ArrayList<>(keySentences);
        for(String sentence : sentences){
            for(int i = 0;i<keySentencesCopy.size();i++){
                if(keySentencesCopy.get(i).getKey().equals(sentence)){
                    keySentencesCopy.remove(i);
                    for(String sub : subs){
                        if(sentence.startsWith(sub)){
                            if(result.size() == 0 || !result.get(result.size()-1).getKey().equals(sub)){
                                if(sub.length() < sentence.length()){
                                    Pair<String, List<String>> pair = new Pair<>(sub,new ArrayList<>());
                                    pair.getValue().add(sentence.substring(sub.length()+1));
                                    result.add(pair);
                                }else{
                                    continue;
                                }
                            }else{
                                if(sub.length() < sentence.length()){
                                    result.get(result.size()-1).getValue().add(sentence.substring(sub.length()+1));
                                }else{
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

}
