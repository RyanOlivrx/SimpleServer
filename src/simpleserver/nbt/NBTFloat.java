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
package simpleserver.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NBTFloat extends NBTag {
  private Float value;

  NBTFloat() {
    super();
  }

  NBTFloat(DataInputStream in, Boolean named) throws Exception {
    super(in, named);
  }

  public NBTFloat(float value) {
    set(value);
  }

  public NBTFloat(String name, float value) {
    super(name);
    set(value);
  }

  @Override
  protected byte id() {
    return 5;
  }

  @Override
  public Float get() {
    return value;
  }

  @Override
  void set(String value) {
    this.value = Float.valueOf(value);
  }

  public void set(float value) {
    this.value = value;
  }

  @Override
  protected void loadValue(DataInputStream in) throws IOException {
    value = in.readFloat();
  }

  @Override
  protected void saveValue(DataOutputStream out) throws IOException {
    out.writeFloat(value);
  }
}
