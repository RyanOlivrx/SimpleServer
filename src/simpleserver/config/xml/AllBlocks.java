/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.config.xml;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class AllBlocks extends XMLTag {
  public String place;
  public String destroy;
  public String use;
  public String take;
  public String give;

  private static final String PLACE = "place";
  private static final String DESTROY = "destroy";
  private static final String USE = "use";
  private static final String GIVE = "give";
  private static final String TAKE = "take";

  public AllBlocks() {
    super("allblocks");
  }

  @Override
  protected void setAttribute(String name, String value) throws SAXException {
    if (name.equals(PLACE)) {
      place = value;
    } else if (name.equals(DESTROY)) {
      destroy = value;
    } else if (name.equals(USE)) {
      use = value;
    } else if (name.equals(GIVE)) {
      give = value;
    } else if (name.equals(TAKE)) {
      take = value;
    }
  }

  @Override
  protected void saveAttributes(AttributesImpl attributes) {
    if (place != null) {
      addAttribute(attributes, PLACE, place);
    }
    if (destroy != null) {
      addAttribute(attributes, DESTROY, destroy);
    }
    if (use != null) {
      addAttribute(attributes, USE, use);
    }
    if (take != null) {
      addAttribute(attributes, TAKE, take);
    }
    if (give != null) {
      addAttribute(attributes, GIVE, give);
    }
  }
}