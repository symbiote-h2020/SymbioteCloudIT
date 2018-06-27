package eu.h2020.symbiote.client;

import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;

import java.util.function.Predicate;

public class LambdaCondition<T> extends Condition<T> {

	private Predicate<T> predicate;

	public LambdaCondition(Predicate<T> predicate) {
		super();
		this.predicate = predicate;
	}

	public LambdaCondition(Description description, Predicate<T> predicate) {
		super(description);
		this.predicate = predicate;
	}

	public LambdaCondition(String description, Predicate<T> predicate) {
		super(description);
		this.predicate = predicate;
	}

	@Override
	public boolean matches(T value) {
		return predicate.test(value);
	}
}