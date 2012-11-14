package search;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class JapaneseEnglishAnalyzer extends JapaneseAnalyzer {
  private final Mode mode;
  private final Set<String> stoptags;
  private final UserDictionary userDict;

  /**
   * コンストラクタ<br>
   * ==========<br>
   * 使用例<br>
   * analyzer = new JapaneseEnglishAnalyzer(Version.LUCENE_36, null,JapaneseTokenizer.Mode.SEARCH, stopwords, stoptags);<br>
   * ==========<br>
   * stopwordの設定例<br>
   * // ストップワードの設定<br>
   * public static void setStopWords() {<br>
   * stopwords = new CharArraySet(Version.LUCENE_36, StandardAnalyzer.STOP_WORDS_SET, true);<br>
   * stopwords.addAll(JapaneseAnalyzer.getDefaultStopSet());<br>
   * }<br>
   * ==========<br>
   * @param matchVersion luceneのバージョン
   * @param userDict ユーザ辞書
   * @param mode kuromojiのモード．SEARCHモード推奨
   * @param stopwords 英語と日本語のストップワードを格納したSetを渡して下さい．
   * @param stoptags 日本語のストップタグ
   */
  public JapaneseEnglishAnalyzer(Version matchVersion, UserDictionary userDict,
      Mode mode, CharArraySet stopwords, Set<String> stoptags) {
    super(matchVersion, userDict, mode, stopwords, stoptags);
    this.userDict = userDict;
    this.mode = mode;
    this.stoptags = stoptags;
  }

  /**
   * KStemFilterによる英語ステミングを追加
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {

    Tokenizer tokenizer = new JapaneseTokenizer(reader, userDict, true, mode);
    TokenStream stream = new JapaneseBaseFormFilter(tokenizer);
    stream = new JapanesePartOfSpeechStopFilter(true, stream, stoptags);
    stream = new CJKWidthFilter(stream);
    stream = new StopFilter(matchVersion, stream, stopwords);
    stream = new JapaneseKatakanaStemFilter(stream);
    stream = new LowerCaseFilter(matchVersion, stream);
    stream = new KStemFilter(stream); // 追加．英語のステミングを行う
    return new TokenStreamComponents(tokenizer, stream);
  }

}
