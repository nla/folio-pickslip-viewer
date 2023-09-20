package au.gov.nla.pickslip;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("unicodemap")
public class UnicodeFontMap {

  static Logger log = LoggerFactory.getLogger(UnicodeFontMap.class);
  private List<FontRange> fontRanges;

  private static HashMap<String, BitSet> charSetByName = new HashMap<>();

  private static int CODE_POINTS = 0x10000; // 10000 for BMP; 20000 for BMP and SMP planes
  private static int CODE_POINT_BYTES = CODE_POINTS >> 3; // our storage req'd (bytes)

  private static void storeFor(String name, String charSet) {

    BitSet cmap;
    byte[] bytes = new byte[CODE_POINT_BYTES]; // 8192 bytes, 0..ffff bits for plane 0

    String[] lines = charSet.split("\n");
    for (String line : lines) {
      String[] p = line.split(" ");
      String offset = p[0].substring(0, p[0].length() - 1);

      // each row contains 8 (32 bit) double words - i.e. 32 bytes
      int lOffset = Integer.valueOf(offset, 16) << 5;

      // skip if there are glyphs defined past our maximum
      if (lOffset + 32 > CODE_POINT_BYTES) {
        break;
      }

      for (int i = 0; i < 8; i++) { // 8 columns
        long v = Long.valueOf(p[i + 1], 16); // v is a double word
        byte b0 = (byte) ((v & 0xff000000L) >>> 24);
        byte b1 = (byte) ((v & 0x00ff0000L) >>> 16);
        byte b2 = (byte) ((v & 0x0000ff00L) >>> 8);
        byte b3 = (byte) (v & 0x000000ffL);

        // little endian (32 bit)
        bytes[lOffset + i * 4] = b3;
        bytes[lOffset + i * 4 + 1] = b2;
        bytes[lOffset + i * 4 + 2] = b1;
        bytes[lOffset + i * 4 + 3] = b0;
      }
    }
    cmap = BitSet.valueOf(bytes);

    UnicodeFontMap.charSetByName.put(name, cmap);
  }

  private int countMatches(String script, BitSet cmap) {
    int m = 0;
    if (script != null) {
      for (char c : script.toCharArray()) {
        m += cmap.get(c) ? 1 : 0;
      }
    }
    return m;
  }

  private FontRange getBestFontRangeFor(String script) {

    HashMap<String, Integer> matches = new HashMap<>();

    UnicodeFontMap.charSetByName.forEach(
        (String name, BitSet cmap) -> {
          matches.put(name, countMatches(script, cmap));
        });

    Map.Entry<String, Integer> me =
        matches.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);

    return (me == null || me.getValue() == 0)
        ? null
        : fontRanges.stream().filter(f -> f.name.equals(me.getKey())).findFirst().orElse(null);
  }

  // Search all installed fonts for the one that can best render the given string (i.e. most
  // glyphs).
  public String getBestFontFamilyFor(String script) {
    FontRange fontRange = getBestFontRangeFor(script);
    return (fontRange == null ? null : fontRange.fontFamily);
  }

  public record FontRange(String name, String fontFamily, String charset) {

    public FontRange {
      storeFor(name, charset);
    }
  }
  ;

  public void setFontRanges(List<FontRange> ranges) {
    this.fontRanges = ranges;
  }
}
