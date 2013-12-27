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

package ratpack.groovy.launch;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import ratpack.groovy.internal.RatpackDslClosureToHandlerTransformer;
import ratpack.groovy.internal.ScriptBackedApp;
import ratpack.groovy.server.internal.GroovyKitAppFactory;
import ratpack.guice.Guice;
import ratpack.guice.internal.GuiceBackedHandlerFactory;
import ratpack.handling.Handler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.util.Transformer;

import java.io.File;
import java.io.IOException;

public class GroovyScriptFileHandlerFactory implements HandlerFactory {

  public static final String SCRIPT_PROPERTY_NAME = "groovy.script";
  public static final String SCRIPT_PROPERTY_DEFAULT = "ratpack.groovy";

  public static final String COMPILE_STATIC_PROPERTY_NAME = "groovy.compileStatic";
  public static final String COMPILE_STATIC_PROPERTY_DEFAULT = "false";

  public Handler create(LaunchConfig launchConfig) {
    String scriptName = launchConfig.getOther(SCRIPT_PROPERTY_NAME, SCRIPT_PROPERTY_DEFAULT);
    File script = new File(launchConfig.getBaseDir(), scriptName);

    if (!script.exists()) {
      File capitalized = new File(launchConfig.getBaseDir(), scriptName.substring(0, 1).toUpperCase() + scriptName.substring(1));
      if (capitalized.exists()) {
        script = capitalized;
      }
    }

    try {
      script = script.getCanonicalFile();
    } catch (IOException ignore) {
      // ignore
    }

    boolean compileStatic = Boolean.parseBoolean(launchConfig.getOther(COMPILE_STATIC_PROPERTY_NAME, COMPILE_STATIC_PROPERTY_DEFAULT));

    Transformer<Module, Injector> moduleTransformer = Guice.newInjectorFactory(launchConfig);
    GuiceBackedHandlerFactory handlerFactory = new GroovyKitAppFactory(launchConfig);
    Transformer<Closure<?>, Handler> closureTransformer = new RatpackDslClosureToHandlerTransformer(launchConfig, handlerFactory, moduleTransformer);

    return new ScriptBackedApp(script, compileStatic, launchConfig.isReloadable(), closureTransformer);
  }

}