package index;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import search.AnalyzerFactory;

/**
 * Tweets2011corpus向け，索引作成プログラム.
 * 英語以外の言語を含む，全tweetを索引付けします．<br>
 * 索引づけされるデータは，タブ区切りで以下の順序を想定しています．<br>
 * tweetID  UserName  status  date  text
 */
public class IndexMaker {
  public static final String TWEET_ID = "tweet_id";
  public static final String USER_ID  = "user_id";
  public static final String DATE     = "datetime";
  public static final String TEXT     = "text";

  /** 索引を作るディレクトリ */
  private String pathOfIndex;
  /** アナライザ */
  private Analyzer analyzer;

  /**
   * コンストラクタ
   * @param pathOfIndex 索引を作るディレクトリ
   * @param analyzer アナライザ
   */
  public IndexMaker(String pathOfIndex, Analyzer analyzer) {
    setPathOfIndex(pathOfIndex);
    setAnalyzer(analyzer);
  }

  /**
   * ファイルをインデックスに追加する．
   * @param filePath インデックスに加えたいファイル
   * @throws IOException
   */
  public void addFileToIndex(String filePath) throws IOException {
    // インデックスディレクトリを開く
    Directory index = FSDirectory.open(new File(pathOfIndex));
    // IndexWriterクラスの設定クラス
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    // IndexWriterを作る
    IndexWriter writer = new IndexWriter(index, config);

    LineIterator li = null;
    try {
      li = FileUtils.lineIterator(new File(filePath), "utf-8");
      while (li.hasNext()) {
        String line = li.nextLine();
        String[] lineAry = line.split("\t");
        Document document = new Document();
        // tweet_id (値取り出し可，検索不可)
        document.add(new Field(IndexMaker.TWEET_ID, lineAry[0], Field.Store.YES, Field.Index.NO));
        // user_id（値取り出し可，検索不可）
        document.add(new Field(IndexMaker.USER_ID, lineAry[1], Field.Store.YES, Field.Index.NO));
        // date（値取り出し可，検索不可）
        document.add(new Field(IndexMaker.DATE, lineAry[3], Field.Store.YES, Field.Index.NO));
        // text（値取り出し可，検索可）
        document.add(new Field(IndexMaker.TEXT, lineAry[4], Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(document);
      }
    } catch (IOException e) {
      System.err.println("error: faield to read input files :" + filePath);
      e.printStackTrace();
    }
    writer.commit();
    writer.close();
    index.close();
  }

  /**
   * 索引を作るディレクトリを取得します。
   * @return pathOfIndex 索引を作るディレクトリ
   */
  public String getPathOfIndex() {
      return pathOfIndex;
  }

  /**
   * 索引を作るディレクトリを設定します。
   * @param pathOfIndex 索引を作るディレクトリ
   */
  public void setPathOfIndex(String pathOfIndex) {
      this.pathOfIndex = pathOfIndex;
  }

  /**
   * アナライザを取得します。
   * @return analyzer アナライザ
   */
  public Analyzer getAnalyzer() {
      return analyzer;
  }

  /**
   * アナライザを設定します。
   * @param analyzer アナライザ
   */
  public void setAnalyzer(Analyzer analyzer) {
      this.analyzer = analyzer;
  }

  /**
   * インデックスを作成
   * @param args[0] 索引を作るディレクトリ
   * @param args[1] 索引付けするファイル
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("error: 引数は2つだけ指定してください．");
      System.exit(0);
    }
    System.out.println("indexDirectory : " + args[0]);
    System.out.println("add File : " + args[1]);
    AnalyzerFactory analyzerFactory = new AnalyzerFactory(Version.LUCENE_40);
    Analyzer analyzer = analyzerFactory.getJapaneseEnglishAnalyzerSearch();
    IndexMaker indexMaker = new IndexMaker(args[0], analyzer);
    try {
      indexMaker.addFileToIndex(args[1]);
    } catch (IOException e) {
      System.err.println("error: faild to make index");
      e.printStackTrace();
    }

  }

}
