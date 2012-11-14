package search;

import index.IndexMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Luceneの検索プログラム．
 * ワークショップで使ったものをTweets2011corpus向けに少し手直ししたものです．
 * tagcloudやSolr検索部分はそのままなので，恐らくTweets2011の索引では使えません．<br>
 * 引数に，IndexMakerで作成したインデックスの場所を指定して実行すると検索できると思います．
 */
public class RunQuery {
  public static final String SCORE    = "score";
  public static final String TWEET_ID = "tweet_id";
  public static final String USER_ID  = "userid";
  public static final String DATE     = "date";
  public static final String TEXT     = "tweet";

  // for Solr
	public static final String POST_ENCODING = "UTF-8";
  protected static URL solrUrl = null;
  private String server = null;

	// for Lucene
	private Directory     indexDir   = null;
  private IndexReader   reader     = null;
  private IndexSearcher searcher   = null;
  private Similarity    similarity = null;
  private Analyzer      analyzer   = null;
  private String        field      = null;
  private String        index      = null;

  /**
   * Luceneのインデックスをセット．
   * @param index_
   */
	public RunQuery(String index_) {
		this.index = index_;
	}

	/**
	 * SolrのリクエストURLをセット．
	 * @param hostname
	 * @param port
	 */
	public RunQuery(String hostname, int port) {
	  this.server = "http://" + hostname + ":" + Integer.toString(port) + "/solr/collection1/select?wt=json&indent=true&fl=*%2Cscore&rows=1000&q=text%3A";
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getIndexPath() {
		return this.index;
	}

	public String getServerName() {
		return this.server;
	}

	/**
	 * クエリ文字列を合成する．
	 * リスト内のクエリをスペース区切りの文字列にする．
	 * @param qs クエリ文字列のリスト
	 * @param filed 検索するフィールド
	 * @return 合成されたクエリ文字列
	 */
	public String qs2query(List<String> qs, String filed) {
    if (filed != null) {
      this.field = filed;
    } else {
      this.field = "text";
    }
	  StringBuffer sb = new StringBuffer();
    for (String q : qs ) {
      sb.append(" " + q);
    }
    return sb.toString();
	}

	/**
	 * SolrまたはLuceneを使った検索を行なって結果を返す
	 * @param query 合成済みクエリ
	 * @param retnum
	 * @return 検索結果
	 */
	public List<Map> search(String query, int retnum) {
    String[] names;
    List<Map> list = new ArrayList<Map>();
    HttpURLConnection urlc = null;
    try {
      if (index != null) {  // indexが設定されてる時はLuceneで検索
        System.out.println("search by Lucene");
        indexDir = FSDirectory.open(new File(index));
        reader   = DirectoryReader.open(indexDir);
        searcher = new IndexSearcher(reader);
        //similarity = new BM25Similarity();
        similarity = new LMDirichletSimilarity(10000f);
        //similarity = new DefaultSimilarity();
        searcher.setSimilarity(similarity);
        analyzer = new AnalyzerFactory(Version.LUCENE_40).getJapaneseEnglishAnalyzerSearch();
        System.out.println("QUERY : " + query + "(" + retnum + ")");
        System.out.println("Index : " + this.index);
        System.out.println("Server : " + this.server);

        list = searchByLucene(query, retnum);

        indexDir.close();
        reader.close();
      } else if (server != null) { // サーバがセットされている時はSolrで検索
        System.out.println("search by Solr");
        System.out.println("sorry. You cannot search by Solr.");
        /*
        // リクエストURLの作成
        StringBuffer sb = new StringBuffer();
        sb.append(server);
        sb.append(URLEncoder.encode(query, POST_ENCODING));
        solrUrl = new URL(sb.toString());
        // リクエスト
        urlc = (HttpURLConnection) solrUrl.openConnection();
        urlc.setRequestMethod("GET");
        urlc.setRequestProperty("Content-type", "text/xml; charset=" + POST_ENCODING);
        urlc.connect();
        // リクエスト結果受信
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(urlc.getInputStream(), POST_ENCODING));
        // 結果の処理
        int lineNum = 0;
        int lineNum2 = 0;
        Map map = new HashMap<String, Object>();
        while (true) {
          String line = reader.readLine(); // 結果を1行ずつ読み込む
          lineNum++;
          if ( line == null ) {
            break;
          }
          if ( lineNum < 12 ) {
            continue;
          }
          lineNum2++;
          if ( lineNum2 == 2 ) {
            String tweet_id = line.substring(20, line.length()-2);
            map.put(RunQuery.TWEET_ID, tweet_id);
          } else if ( lineNum2 == 3 ) {
            String user_id = line.substring(19, line.length()-2);
            map.put(RunQuery.USER_ID, user_id);
          } else if (lineNum2 == 4) {
            String datetimeStr = line.substring(20, line.length()-2);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date datetime = sdf.parse(datetimeStr);
            map.put(RunQuery.DATE, datetime);
          } else if (lineNum2 == 5) {
            String text = line.substring(16, line.length()-2);
            map.put(RunQuery.TEXT, text);
          } else if (lineNum2 == 6) {
            String score = line.substring(16, line.length()-2);
            map.put(RunQuery.SCORE, Double.parseDouble(score));
          } else if (lineNum2 == 7) {
            lineNum2 = 1;
            list.add(map);
            map = new HashMap<String, Object>();
          }
        }
        */
      } else {
        System.err.println("You should specify index or server.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // 終了処理
      if (urlc != null) {
        urlc.disconnect();
      }
    }
    return list;
	}

	/**
	 * Luceneを使った検索を行なって，結果のListを返す．
	 * @param query 合成済みクエリ
	 * @param　retnum
	 * @return 検索結果
	 * @throws Exception
	 */
	protected List<Map> searchByLucene(String query, int retnum) throws Exception {
	  List<Map> list = new ArrayList<Map>();

    QueryParser qp = new QueryParser(Version.LUCENE_40, field, analyzer);
    Query luceneQuery = qp.parse(query);
    TopDocs topDocs = searcher.search(luceneQuery, retnum);
    ScoreDoc[] hits = topDocs.scoreDocs;

    System.out.println("RetNum : " + hits.length);

    for (int i=0; i<hits.length; i++) {
      int docId = hits[i].doc;
      Document doc = searcher.doc(docId);
      /*
      Tweets2011の日付フォーマット　Sun Jan 23 23:06:09 +0000 2011
      下記のようにしてもDateにparse出来なかったので，Stringにしました．
      SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
      Date datetime = sdf.parse(doc.get(IndexMaker.DATE), pos);
      */

      Map map = new HashMap<String, String>();
      map.put(RunQuery.SCORE, new Double(hits[i].score));
      map.put(RunQuery.TWEET_ID, (String) doc.get(IndexMaker.TWEET_ID));
      map.put(RunQuery.USER_ID, (String) doc.get(IndexMaker.USER_ID));
      map.put(RunQuery.DATE, (String) doc.get(IndexMaker.DATE));
      map.put(RunQuery.TEXT, (String) doc.get(IndexMaker.TEXT));
      list.add(map);
    }
    return list;
	}

	public Map<String, Double> tagcloud(Map<String, Double> map_) {
	  //System.out.println("############### RunQuery.tagcloud #################");
	  //System.out.println("size of map_ = " + map_.size());
		Map<String, Double> map = new HashMap<String, Double>();
		double Z = 0;
		double[] vs = ArrayUtils.toPrimitive(map_.values().toArray(
				new Double[0]));
		double v_max = StatUtils.max(vs);
		double v_min = StatUtils.min(vs);
		//System.out.println("vs = " + vs);
		//System.out.println("size of vs = " + vs.length);
		//System.out.println("v_max = " + v_max);
		//System.out.println("v_min = " + v_min);

		for (String term : map_.keySet()) {
			double score = ((map_.get(term) - v_min) / (v_max - v_min) + 0.1) * 10;
			map.put(term, score);
			System.out.print(term + " ");
		}
		System.out.println();
		//System.out.println("####################################################");
		return map;
	}

	private Map sortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static void printMap(Map mp) {
		Iterator it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			if ((Integer) pairs.getValue() == 1) {
				continue;
			}
			System.out.println("'" + pairs.getKey() + " = " + pairs.getValue());
			it.remove(); // avoids a ConcurrentModificationException
		}
	}

	/**
	 * Make temporal profile from retrieved tweets
	 *
	 * @param tweets
	 * @return
	 */
	public Map<Date, Float> tweets2tp(List<Map> tweets) {
		Map<Date, Float> dateMap = new HashMap<Date, Float>();
		for (Map tweet : tweets) {
			try {
				String date_str = (String) tweet.get("date");
				DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
				Date date = df.parse(date_str);
				// System.out.println(date);
				// System.exit(-1);
				Float count = dateMap.get(date);
				dateMap.put(date, count == null ? 1 : count + 1);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return dateMap;
	}

	/**
	 * Make chart for the temporal profile of a word
	 *
	 * @param tp_map
	 * @return
	 */
	public List chart(Map<String, Map> tp_map) {
		List tps = new ArrayList();
		Map<String, Map> tpsMap = new HashMap<String, Map>();
		Set tps_ = new HashSet();

		for (String q : tp_map.keySet()) {
			Map<Date, Double> tp = tp_map.get(q);
			tpsMap.put(q, tp);
			tps_.addAll(tp.keySet());
		}

		Date dayMax = Collections.max(tps_);
		Date dayMin = Collections.min(tps_);
		dayMax = DateUtils.addDays(dayMax, 1);
		dayMin = DateUtils.addDays(dayMin, -1);

		List days = new ArrayList();
		for (Date day = dayMin; day.getTime() <= dayMax.getTime(); day = DateUtils
				.addDays(day, 1)) {
			days.add(day);
			Map<String, Object> tp_norm = new HashMap<String, Object>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			tp_norm.put("time", sdf.format(day));

			for (String q : tpsMap.keySet()) {
				Map<Date, Double> tp = tpsMap.get(q);
				Double count = tp.get(day);
				count = (count == null ? 0 : count / sum(tp.values()));
				tp_norm.put(q, count);
			}
			tps.add(tp_norm);
		}
		return tps;
	}

	private double sum(Collection<Double> cols) {
		double sum = StatUtils.sum(ArrayUtils.toPrimitive(cols
				.toArray(new Double[0])));
		return sum;
	}

	/**
	 * 引数のクエリリストに少なくとも1つクエリが含まれているかを調べる.<br>
	 * @param qs
	 * @return クエリリストに少なくとも1つクエリが含まれるときtrue / すべて空またはnullのときfalse
	 */
	public boolean queryCheck(List<String> qs) {
		for (String q : qs) {
			if (q != "" && q != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param args[0] 索引のパス
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
    List<String> query = new ArrayList<String>();
    query.add("fifa");
    query.add("soccer");
    query.add("2022");
    int retnum = 10;

    RunQuery rq = new RunQuery(args[0]);
    List<Map> tweets = rq.search(rq.qs2query(query, null), retnum);
    for (Map tweet : tweets) {
      Object[] t = { tweet.get(RunQuery.SCORE), tweet.get(RunQuery.TWEET_ID),
          tweet.get(RunQuery.DATE), tweet.get(RunQuery.TEXT) };
      System.out.println(StringUtils.join(t, "\t"));
    }
	}
}
