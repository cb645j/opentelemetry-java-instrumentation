package io.opentelemetry.javaagent.instrumentation.camunda.v7_18.jobs;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;

import java.util.Optional;

import static io.opentelemetry.javaagent.instrumentation.camunda.v7_18.jobs.CamundaJobSingletons.getInstumenter;
import static io.opentelemetry.javaagent.instrumentation.camunda.v7_18.jobs.CamundaJobSingletons.getOpentelemetry;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import io.opentelemetry.javaagent.instrumentation.camunda.v7_18.common.CamundaCommonRequest;
import io.opentelemetry.javaagent.instrumentation.camunda.v7_18.jobs.CamundaExecutionEntityGetter;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class CamundaAsyncContinuationJobHandlerInstrumentation implements TypeInstrumentation {

	@Override
	public ElementMatcher<ClassLoader> classLoaderOptimization() {
		return hasClassesNamed("org.camunda.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler");
	}

	@Override
	public ElementMatcher<TypeDescription> typeMatcher() {
		return ElementMatchers.named("org.camunda.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler");
	}

	@Override
	public void transform(TypeTransformer transformer) {
		transformer
				.applyAdviceToMethod(
						ElementMatchers.isMethod().and(ElementMatchers.named("execute"))
								.and(ElementMatchers.takesArgument(1,
										ElementMatchers.named(
												"org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity"))),
						this.getClass().getName() + "$CamundaAsyncContinuationJobHandlerAdvice");
	}

	public static class CamundaAsyncContinuationJobHandlerAdvice {
		@Advice.OnMethodEnter(suppress = Throwable.class)
		public static void addTracingEnter(@Advice.Argument(1) ExecutionEntity executionEntity,
				@Advice.Local("request") CamundaCommonRequest request,
				@Advice.Local("otelParentScope") Scope parentScope, @Advice.Local("otelContext") Context context,
				@Advice.Local("otelScope") Scope scope) {


			if (executionEntity == null) {
				return;
			}

			request = new CamundaCommonRequest();
			request.setProcessDefinitionId(Optional.ofNullable(executionEntity.getProcessDefinitionId()));
			if (executionEntity.getProcessDefinition() != null) {
				request.setProcessDefinitionKey(Optional.ofNullable(executionEntity.getProcessDefinition().getKey()));
			}
			request.setProcessInstanceId(Optional.ofNullable(executionEntity.getProcessInstanceId()));
			request.setActivityId(Optional.ofNullable(executionEntity.getActivityId()));
			if (executionEntity.getActivity() != null) {
				request.setActivityName(Optional.ofNullable(executionEntity.getActivity().getName()));
			}

			String processInstanceId = executionEntity.getProcessInstanceId();

			if (Java8BytecodeBridge.currentContext() == Java8BytecodeBridge.rootContext()) {
				System.out.println("No initial span context for process instance " + processInstanceId);
			}

			Context parentContext = getOpentelemetry().getPropagators().getTextMapPropagator()
					.extract(Java8BytecodeBridge.currentContext(), executionEntity, new CamundaExecutionEntityGetter());

			parentScope = parentContext.makeCurrent();

			if (getInstumenter().shouldStart(Java8BytecodeBridge.currentContext(), request)) {
				context = getInstumenter().start(Java8BytecodeBridge.currentContext(), request);
				scope = context.makeCurrent();

			} else {
				System.out.println("Unable to start telemetry for process " + processInstanceId);
			}

		}

		@Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
		public static void closeTrace(@Advice.Argument(1) ExecutionEntity executionEntity,
				@Advice.Local("request") CamundaCommonRequest request,
				@Advice.Local("otelParentScope") Scope parentScope, @Advice.Local("otelContext") Context context,
				@Advice.Local("otelScope") Scope scope, @Advice.Thrown Throwable throwable) {


			if (context != null && scope != null) {
				getInstumenter().end(context, request, "NA", throwable);
				scope.close();
			}

			if (parentScope != null) {
				parentScope.close();
			}

		}
	}

}
