/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.session;

import ratpack.util.Exceptions;

import java.io.*;

public class JavaSerializationSessionValueSerializer implements SessionValueSerializer {

  @Override
  public <T> void serialize(Class<T> type, T value, OutputStream outputStream) throws IOException {
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(value);
    objectOutputStream.flush();
  }

  @Override
  public <T> T deserialize(Class<T> type, InputStream inputStream) throws IOException {
    try {
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object value = objectInputStream.readObject();
      if (type.isInstance(value)) {
        return type.cast(value);
      } else {
        throw new ClassCastException("Expected to read object of type " + type.getName() + " from string, but got: " + value.getClass().getName());
      }
    } catch (ClassNotFoundException e) {
      throw Exceptions.uncheck(e);
    }
  }
}
