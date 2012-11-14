package search;

import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class AnalyzerFactory {
  /** Luceneのバージョン */
  private Version version;

  /**
   * コンストラクタ
   * @param version Luceneのバージョン
   */
  public AnalyzerFactory(Version version) {
    setVersion(version);
  }

  /**
   * デフォルト設定+サーチモードのJapaneseEnglishAnalyzerを返す
   * @return デフォルト設定のJapaneseEnglishAnalyzer
   */
  @Deprecated
  public Analyzer getJapaneseEnglishAnalyzer() {
    CharArraySet stopwords = getStopWordsCharArraySet();
    Analyzer analyzer = new JapaneseEnglishAnalyzer(version, null, JapaneseTokenizer.Mode.SEARCH, stopwords, JapaneseAnalyzer.getDefaultStopTags());
    return analyzer;
  }

  /**
   * デフォルト設定+ノーマルモードのJapaneseEnflishAnalyzerを返す
   * @return デフォルト設定+ノーマルモードのJapaneseAnalyzer
   */
  public Analyzer getJapaneseEnglishAnalyzerNormal() {
    CharArraySet stopwords = getStopWordsCharArraySet();
    Analyzer analyzer = new JapaneseEnglishAnalyzer(version, null, JapaneseTokenizer.Mode.NORMAL, stopwords, JapaneseAnalyzer.getDefaultStopTags());
    return analyzer;
  }

  /**
   * デフォルト設定+サーチモードのJapaneseEnglishAnalyzerを返す
   * @return デフォルト設定のJapaneseEnglishAnalyzer
   */
  public Analyzer getJapaneseEnglishAnalyzerSearch() {
    CharArraySet stopwords = getStopWordsCharArraySet();
    Analyzer analyzer = new JapaneseEnglishAnalyzer(version, null, JapaneseTokenizer.Mode.SEARCH, stopwords, JapaneseAnalyzer.getDefaultStopTags());
    return analyzer;
  }

  /**
   * デフォルト設定+拡張モードのJapaneseEnglishAnalyzerを返す
   * @return デフォルト設定のJapaneseEnglishAnalyzer
   */
  public Analyzer getJapaneseEnglishAnalyzerExtended() {
    CharArraySet stopwords = getStopWordsCharArraySet();
    Analyzer analyzer = new JapaneseEnglishAnalyzer(version, null, JapaneseTokenizer.Mode.EXTENDED, stopwords, JapaneseAnalyzer.getDefaultStopTags());
    return analyzer;
  }


  public Analyzer getTwitterSearchAnalyzer() {
    CharArraySet stopwords = getStopWordsCharArraySet();
    stopwords.addAll(stopwords);
    Analyzer analyzer = new JapaneseEnglishAnalyzer(version, null, JapaneseTokenizer.Mode.SEARCH, stopwords, JapaneseAnalyzer.getDefaultStopTags());
    return analyzer;
  }

  /**
   * StandardAnalyzerを返す
   * @return でフォルト設定のStandardAnalyzer
   */
  public Analyzer getStaAnalyzer() {
    Analyzer analyzer = new StandardAnalyzer(version);
    return analyzer;
  }

  /**
   * 日本語と英語のストップワードのCharArraySetを返す
   * @return 日本語と英語のストップワードのCharArraySet
   */
  public CharArraySet getStopWordsCharArraySet() {
    CharArraySet stopwords = new CharArraySet(version, StandardAnalyzer.STOP_WORDS_SET, true);
    stopwords.addAll(JapaneseAnalyzer.getDefaultStopSet());
    return stopwords;
  }

  public Version getVersion() {
    return version;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

}
