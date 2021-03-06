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

import ratpack.exec.Operation;
import ratpack.exec.Promise;

import java.util.Optional;
import java.util.Set;

// Will eventually replace Session
public interface SessionAdapter {

  Promise<SyncSession> getSync();

  default Promise<Optional<String>> get(String key) {
    return getSync().map(s -> s.get(key));
  }

  default Promise<String> require(String key) {
    return getSync().map(s -> s.require(key));
  }

  default <T> Promise<Optional<? extends T>> get(Class<T> key) {
    return getSync().map(s -> s.get(key));
  }

  default <T> Promise<T> require(Class<T> key) {
    return getSync().map(s -> s.require(key));
  }

  default <T> Promise<Optional<? extends T>> get(Class<T> key, SessionValueSerializer serializer) {
    return getSync().map(s -> s.get(key, serializer));
  }

  default <T> Promise<T> require(Class<T> key, SessionValueSerializer serializer) {
    return getSync().map(s -> s.require(key, serializer));
  }

  default Operation set(String key, String value) {
    return getSync().operation(s -> s.set(key, value));
  }

  default <T> Operation set(Class<T> key, T value) {
    return getSync().operation(s -> s.set(key, value));
  }

  default <T> Operation set(Class<T> key, T value, SessionValueSerializer serializer) {
    return getSync().operation(s -> s.set(key, value, serializer));
  }

  default <T> Operation set(T value) {
    return getSync().operation(s -> s.set(value));
  }

  default <T> Operation set(T value, SessionValueSerializer serializer) {
    return getSync().operation(s -> s.set(value, serializer));
  }

  default Promise<Set<String>> getStringKeys() {
    return getSync().map(SyncSession::getStringKeys);
  }

  default Promise<Set<Class<?>>> getTypeKeys() {
    return getSync().map(SyncSession::getTypeKeys);
  }

  default Operation remove(String key) {
    return getSync().operation(s -> s.remove(key));
  }

  default <T> Operation remove(Class<T> key) {
    return getSync().operation(s -> s.remove(key));
  }

  default Operation clear() {
    return getSync().operation(SyncSession::clear);
  }

  // Has the session been changed (i.e. set/remove/clear called) since read?
  boolean isDirty();

  // Store the session data right now - doesn't have to be called - we'll call automatically at end of request if dirty
  Operation save();

  Operation terminate();
}
