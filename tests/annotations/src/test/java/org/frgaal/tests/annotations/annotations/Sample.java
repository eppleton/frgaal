package org.frgaal.tests.annotations.annotations;

@Ann(v1=12, v2="13", v3={@Add("add-first"), @Add({"add-second", "add-third"})})
public class Sample {
}
