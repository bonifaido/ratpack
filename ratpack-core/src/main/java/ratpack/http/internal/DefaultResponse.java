/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http.internal;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Publisher;
import ratpack.api.Nullable;
import ratpack.exec.ExecControl;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.http.*;
import ratpack.util.MultiValueMap;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static ratpack.http.internal.HttpHeaderConstants.CONTENT_TYPE;

public class DefaultResponse implements Response {

  private HttpResponseStatus status = HttpResponseStatus.OK;
  private final MutableHeaders headers;
  private final ByteBufAllocator byteBufAllocator;
  private final ResponseTransmitter responseTransmitter;

  private boolean contentTypeSet;
  private Set<Cookie> cookies;
  private List<Action<? super ResponseMetaData>> responseFinalizers;

  public DefaultResponse(MutableHeaders headers, ByteBufAllocator byteBufAllocator, ResponseTransmitter responseTransmitter) {
    this.byteBufAllocator = byteBufAllocator;
    this.responseTransmitter = responseTransmitter;
    this.headers = new MutableHeadersWrapper(headers);
    this.responseFinalizers = Lists.newArrayList();
  }

  class MutableHeadersWrapper implements MutableHeaders {

    private final MutableHeaders wrapped;

    MutableHeadersWrapper(MutableHeaders wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public MutableHeaders add(CharSequence name, Object value) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.add(name, value);
      return this;
    }

    @Override
    public MutableHeaders set(CharSequence name, Object value) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.set(name, value);
      return this;
    }

    @Override
    public MutableHeaders setDate(CharSequence name, Date value) {
      wrapped.set(name, value);
      return this;
    }

    @Override
    public MutableHeaders set(CharSequence name, Iterable<?> values) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.set(name, values);
      return this;
    }

    @Override
    public MutableHeaders remove(CharSequence name) {
      if (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString())) {
        contentTypeSet = false;
      }

      wrapped.remove(name);
      return this;
    }

    @Override
    public MutableHeaders clear() {
      contentTypeSet = false;
      wrapped.clear();
      return this;
    }

    @Override
    public MutableHeaders copy(Headers headers) {
      this.wrapped.copy(headers);
      if (headers.contains(HttpHeaderConstants.CONTENT_TYPE)) {
        contentTypeSet = true;
      }
      return this;
    }

    @Override
    public MultiValueMap<String, String> asMultiValueMap() {
      return wrapped.asMultiValueMap();
    }

    @Nullable
    @Override
    public String get(CharSequence name) {
      return wrapped.get(name);
    }

    @Nullable
    @Override
    public String get(String name) {
      return wrapped.get(name);
    }

    @Nullable
    @Override
    public Date getDate(CharSequence name) {
      return wrapped.getDate(name);
    }

    @Override
    @Nullable
    public Date getDate(String name) {
      return wrapped.getDate(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
      return wrapped.getAll(name);
    }

    @Override
    public List<String> getAll(String name) {
      return wrapped.getAll(name);
    }

    @Override
    public boolean contains(CharSequence name) {
      return wrapped.contains(name);
    }

    @Override
    public boolean contains(String name) {
      return wrapped.contains(name);
    }

    @Override
    public Set<String> getNames() {
      return wrapped.getNames();
    }

    @Override
    public HttpHeaders getNettyHeaders() {
      return wrapped.getNettyHeaders();
    }
  }

  public Status getStatus() {
    return new DefaultStatus(status);
  }

  public Response status(int code) {
    return status(HttpResponseStatus.valueOf(code));
  }

  @Override
  public Response status(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  @Override
  public Response status(Status status) {
    return status(status.getCode());
  }

  @Override
  public Response noCompress() {
    headers.set(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.IDENTITY);
    return this;
  }

  @Override
  public MutableHeaders getHeaders() {
    return headers;
  }

  public void send() {
    commit(byteBufAllocator.buffer(0, 0));
  }

  @Override
  public Response contentTypeIfNotSet(Supplier<CharSequence> contentType) {
    if (!contentTypeSet) {
      contentType(contentType.get());
    }
    return this;
  }

  @Override
  public Response contentType(CharSequence contentType) {
    headers.set(CONTENT_TYPE, contentType);
    return this;
  }

  public void send(String text) {
    ByteBuf byteBuf = ByteBufUtil.encodeString(byteBufAllocator, CharBuffer.wrap(text), CharsetUtil.UTF_8);
    contentTypeIfNotSet(HttpHeaderConstants.PLAIN_TEXT_UTF8).send(byteBuf);
  }

  public void send(CharSequence contentType, String body) {
    contentType(contentType);
    send(body);
  }

  public void send(byte[] bytes) {
    contentTypeIfNotSet(HttpHeaderConstants.OCTET_STREAM);
    commit(Unpooled.wrappedBuffer(bytes));
  }

  public void send(CharSequence contentType, byte[] bytes) {
    contentType(contentType).send(bytes);
  }

  public void send(CharSequence contentType, ByteBuf buffer) {
    contentType(contentType);
    send(buffer);
  }

  public void send(ByteBuf buffer) {
    contentTypeIfNotSet(HttpHeaderConstants.OCTET_STREAM);
    commit(buffer);
  }

  public void sendFile(Path file) {
    finalizeResponse(responseFinalizers.iterator(), () -> {
      setCookieHeader();
      responseTransmitter.transmit(status, file);
    });
  }

  @Override
  public void sendStream(Publisher<? extends ByteBuf> stream) {
    finalizeResponse(responseFinalizers.iterator(), () -> {
      setCookieHeader();
      stream.subscribe(responseTransmitter.transmitter(status));
    });
  }

  @Override
  public Response beforeSend(Action<? super ResponseMetaData> responseFinalizer) {
    responseFinalizers.add(responseFinalizer);
    return this;
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = Sets.newHashSet();
    }
    return cookies;
  }

  public Cookie cookie(String name, String value) {
    Cookie cookie = new DefaultCookie(name, value);
    getCookies().add(cookie);
    return cookie;
  }

  public Cookie expireCookie(String name) {
    Cookie cookie = cookie(name, "");
    cookie.setMaxAge(0);
    return cookie;
  }

  private void setCookieHeader() {
    if (cookies != null && !cookies.isEmpty()) {
      for (Cookie cookie : cookies) {
        headers.add(HttpHeaderConstants.SET_COOKIE, ServerCookieEncoder.encode(cookie));
      }
    }
  }

  private void commit(ByteBuf buffer) {
    headers.set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
    finalizeResponse(responseFinalizers.iterator(), () -> {
      setCookieHeader();
      responseTransmitter.transmit(status, buffer);
    });
  }

  private void finalizeResponse(Iterator<Action<? super ResponseMetaData>> finalizers, Runnable then) {
    if (finalizers.hasNext()) {
      ExecControl.current().nest(
        () -> finalizers.next().execute(this),
        () -> finalizeResponse(finalizers, then)
      );
    } else {
      then.run();
    }
  }

}
