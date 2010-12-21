/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public abstract class PropertiesConfig extends AbstractConfig {
  private Properties properties;

  protected PropertiesConfig(String filename) {
    super(filename);

    properties = new Properties();
  }

  @Override
  public void load() {
    try {
      InputStream stream = new FileInputStream(getFile());
      try {
        properties.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (FileNotFoundException e) {
      System.out.println(getFilename() + " is missing.  Loading defaults.");
      loadDefaults();
      save();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Failed to load " + getFilename());
    }
  }

  @Override
  public void save() {
    try {
      OutputStream stream = new FileOutputStream(getFile());
      try {
        properties.store(stream, getHeader());
      }
      finally {
        stream.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Failed to save " + getFilename());
    }
  }

  protected String getProperty(String key) {
    return properties.getProperty(key);
  }

  protected String setProperty(String key, String value) {
    return (String) properties.setProperty(key, value);
  }

  protected String removeProperty(String key) {
    return (String) properties.remove(key);
  }

  protected Set<Entry<Object, Object>> entrySet() {
    return properties.entrySet();
  }

  protected Set<Object> keySet() {
    return properties.keySet();
  }

  protected void loadDefaults() {
    InputStream stream = getResourceStream();
    properties = new Properties();
    try {
      try {
        properties.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Failed to load defaults for " + getFilename());
    }
  }
}
