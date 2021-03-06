/*
 * Copyright 2014 the original author or authors.
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

package ratpack.session.clientside;

import io.netty.handler.codec.http.Cookie;
import ratpack.registry.Registry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public interface SessionService {
  String serializeSession(Registry registry, Set<Map.Entry<String, Object>> entries);

  String[] serializeSession(Registry registry, Set<Map.Entry<String, Object>> entries, int maxCookieSize);

  ConcurrentMap<String, Object> deserializeSession(Registry registry, Cookie[] sessionCookies);
}
