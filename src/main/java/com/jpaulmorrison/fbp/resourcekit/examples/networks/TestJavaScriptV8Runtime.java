/*
 * JavaFBP - A Java Implementation of Flow-Based Programming (FBP)
 * Copyright (C) 2009, 2016 J. Paul Morrison
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, see the GNU Library General Public License v3
 * at https://www.gnu.org/licenses/lgpl-3.0.en.html for more details.
 */

package com.jpaulmorrison.fbp.resourcekit.examples.networks;


import java.io.File;

import com.jpaulmorrison.fbp.core.components.io.ReadFile;
import com.jpaulmorrison.fbp.core.components.io.WriteFile;
import com.jpaulmorrison.fbp.core.components.misc.JavaScriptFunction;
import com.jpaulmorrison.fbp.core.components.routing.Output;
import com.jpaulmorrison.fbp.core.engine.Network;

/** 
 * Read file; write to other file
 * 
 */
public class TestJavaScriptV8Runtime extends Network {
  

  @Override
  protected void define() {
	component("JavaScriptV8Runtime", JavaScriptFunction.class);
    connect(component("Read", ReadFile.class), port("OUT"), component("JavaScriptV8Runtime"), port("IN"));
    connect(component("JavaScriptV8Runtime"), port("OUT"), component("Output", Output.class), port("IN"));
    initialize("src/main/resources/testdata/testdata.txt".replace("/", File.separator), component("Read"), port("SOURCE"));
    initialize("function execute(param) {return param + \"hello\";}", component("JavaScriptV8Runtime"), port("CODE"));
   }

  public static void main(final String[] argv) throws Throwable {
    new TestJavaScriptV8Runtime().go();
  }
}
