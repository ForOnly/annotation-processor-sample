package com.foronly.example.annotation.demo;

import com.foronly.example.annotation.BuilderProperty;

/**
 * <p>
 *
 * </p>
 *
 * @author li_cang_long
 * @since 2023/9/6 0:01
 */
public class Person {

	private int age;

	private String name;

	public int getAge() {
		return age;
	}

	@BuilderProperty
	public void setAge(int age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	@BuilderProperty
	public void setName(String name) {
		this.name = name;
	}

}
