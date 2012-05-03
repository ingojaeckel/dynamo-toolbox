package com.ea;

import java.io.Console;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;

public class App {
	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(new AccessModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(Console.class).toInstance(System.console());

				final MethodInterceptor interceptor = new LoadingInterceptor();
				requestInjection(interceptor);

				bindInterceptor(Matchers.any(), Matchers.annotatedWith(LongRunning.class), interceptor);
			}
		});

		final DynamoConsole consoleTest = injector.getInstance(DynamoConsole.class);
		consoleTest.start();
	}
}
