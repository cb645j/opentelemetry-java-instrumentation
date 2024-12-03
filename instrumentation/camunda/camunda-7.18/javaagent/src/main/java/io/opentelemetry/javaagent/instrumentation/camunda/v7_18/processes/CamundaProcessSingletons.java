package io.opentelemetry.javaagent.instrumentation.camunda.v7_18.processes;

import io.opentelemetry.javaagent.instrumentation.camunda.v7_18.common.CamundaCommonRequest;
import io.opentelemetry.javaagent.instrumentation.camunda.v7_18.common.CamundaVariableAttributeExtractor;
import io.opentelemetry.javaagent.instrumentation.camunda.v7_18.processes.CamundaProcessSpanNameExtractor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;

public class CamundaProcessSingletons {

	private static final Instrumenter<CamundaCommonRequest, String> instrumenter;

	private static final OpenTelemetry opentelemetry;

	private static final boolean propagationEnabled;

	static {

		opentelemetry = GlobalOpenTelemetry.get();

		InstrumenterBuilder<CamundaCommonRequest, String> builder = Instrumenter
				.<CamundaCommonRequest, String>builder(opentelemetry, "io.opentelemetry.camunda-process",
						new CamundaProcessSpanNameExtractor())
				.addAttributesExtractor(new CamundaVariableAttributeExtractor());

		instrumenter = builder.buildInstrumenter();
	}

	public static OpenTelemetry getOpentelemetry() {
		return opentelemetry;
	}

	public static Instrumenter<CamundaCommonRequest, String> getInstumenter() {
		return instrumenter;
	}

}
