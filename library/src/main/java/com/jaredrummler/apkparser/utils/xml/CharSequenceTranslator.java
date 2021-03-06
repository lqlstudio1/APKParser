/*
 * Copyright (c) 2015, Jared Rummler
 * Copyright (c) 2015, Liu Dong
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jaredrummler.apkparser.utils.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * An API for translating text.
 * Its core use is to escape and unescape text. Because escaping and unescaping
 * is completely contextual, the API does not present two separate signatures.
 */
abstract class CharSequenceTranslator {

  /**
   * Translate a set of codepoints, represented by an int index into a CharSequence,
   * into another set of codepoints. The number of codepoints consumed must be returned,
   * and the only IOExceptions thrown must be from interacting with the Writer so that
   * the top level API may reliably ignore StringWriter IOExceptions.
   *
   * @param input
   *     CharSequence that is being translated
   * @param index
   *     int representing the current point of translation
   * @param out
   *     Writer to translate the text to
   * @return int count of codepoints consumed
   * @throws IOException
   *     if and only if the Writer produces an IOException
   */
  public abstract int translate(CharSequence input, int index, Writer out) throws IOException;

  /**
   * Helper for non-Writer usage.
   *
   * @param input
   *     CharSequence to be translated
   * @return String output of translation
   */
  public final String translate(CharSequence input) {
    if (input == null) {
      return null;
    }
    try {
      StringWriter writer = new StringWriter(input.length() * 2);
      translate(input, writer);
      return writer.toString();
    } catch (IOException ioe) {
      // this should never ever happen while writing to a StringWriter
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Translate an input onto a Writer. This is intentionally final as its algorithm is
   * tightly coupled with the abstract method of this class.
   *
   * @param input
   *     CharSequence that is being translated
   * @param out
   *     Writer to translate the text to
   * @throws IOException
   *     if and only if the Writer produces an IOException
   */
  public final void translate(CharSequence input, Writer out) throws IOException {
    if (out == null) {
      throw new IllegalArgumentException("The Writer must not be null");
    }
    if (input == null) {
      return;
    }
    int pos = 0;
    int len = input.length();
    while (pos < len) {
      int consumed = translate(input, pos, out);
      if (consumed == 0) {
        char[] c = Character.toChars(Character.codePointAt(input, pos));
        out.write(c);
        pos += c.length;
        continue;
      }
      // contract with translators is that they have to understand codepoints
      // and they just took care of a surrogate pair
      for (int pt = 0; pt < consumed; pt++) {
        pos += Character.charCount(Character.codePointAt(input, pos));
      }
    }
  }

  /**
   * Helper method to create a merger of this translator with another set of
   * translators. Useful in customizing the standard functionality.
   *
   * @param translators
   *     CharSequenceTranslator array of translators to merge with this one
   * @return CharSequenceTranslator merging this translator with the others
   */
  public final CharSequenceTranslator with(CharSequenceTranslator... translators) {
    CharSequenceTranslator[] newArray = new CharSequenceTranslator[translators.length + 1];
    newArray[0] = this;
    System.arraycopy(translators, 0, newArray, 1, translators.length);
    return new AggregateTranslator(newArray);
  }

  /**
   * <p>Returns an upper case hexadecimal <code>String</code> for the given
   * character.</p>
   *
   * @param codepoint
   *     The codepoint to convert.
   * @return An upper case hexadecimal <code>String</code>
   */
  public static String hex(int codepoint) {
    return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
  }

}
