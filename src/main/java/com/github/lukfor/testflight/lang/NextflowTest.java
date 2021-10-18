package com.github.lukfor.testflight.lang;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.function.Executable;

import com.github.lukfor.testflight.util.Command;

import groovy.json.JsonOutput;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public class NextflowTest implements Executable {

	private String name;

	private boolean debug;

	private NextflowTestCode when;

	private NextflowTestCode then;

	private NextflowTestSuite parent;

	public NextflowTest(NextflowTestSuite parent) {
		this.parent = parent;
	}

	public void name(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void when(@DelegatesTo(value = NextflowTest.class, strategy = Closure.DELEGATE_ONLY) final Closure closure) {
		when = new NextflowTestCode(closure);
	}

	public void then(@DelegatesTo(value = NextflowTest.class, strategy = Closure.DELEGATE_ONLY) final Closure closure) {
		then = new NextflowTestCode(closure);
	}

	public void debug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public void execute() throws Throwable {

		NextflowTestContext context = new NextflowTestContext();

		when.execute(context);

		NextflowTestContext.Workflow workflow = runNextflow(parent.getScript(), context.getParams());
		context.setWorkflow(workflow);

		then.execute(context);

	}

	protected NextflowTestContext.Workflow runNextflow(String script, Map<String, Object> params) throws IOException {

		writeParamsJson(params, "params.json");

		Command nextflow = new Command("/usr/local/bin/nextflow", "run", script, "-params-file", "params.json",
				"-ansi-log", "false");
		nextflow.setSilent(!debug);
		int exitCode = nextflow.execute();

		NextflowTestContext.Workflow workflow = new NextflowTestContext.Workflow();

		workflow.exitCode = exitCode;
		workflow.success = (exitCode == 0);
		workflow.failed = (exitCode != 0);

		return workflow;

	}

	protected void writeParamsJson(Map<String, Object> params, String filename) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(JsonOutput.toJson(params));
		writer.close();

	}

}