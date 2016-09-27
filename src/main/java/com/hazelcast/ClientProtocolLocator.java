/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 */
public class ClientProtocolLocator {

    public static void main(String[] args)
            throws ClassNotFoundException {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder().forPackages("com.hazelcast").
                        addScanners(new SubTypesScanner(false)).build()
        );

        Set<Class> requestResponseParamsClasses = reflections.getTypesAnnotatedWith(SuppressFBWarnings.class).stream().filter(klass -> {
            return klass.getName().endsWith("Codec");
        }).flatMap(klass -> {
            return Arrays.asList(klass.getDeclaredClasses()).stream();
        }).collect(Collectors.toSet());

        Map<Class, Set<Field>> paramsByType = new HashMap<>();
        for (Class klass : requestResponseParamsClasses) {
            for (Field f : klass.getDeclaredFields()) {
                // ignore static field named TYPE
                if (f.getName().equals("TYPE")) {
                    continue;
                }
                if (paramsByType.containsKey(f.getType())) {
                    paramsByType.get(f.getType()).add(f);
                } else {
                    Set<Field> fields = new HashSet<>();
                    fields.add(f);
                    paramsByType.put(f.getType(), fields);
                }
            }
        }

        // dump output
        System.out.println("Types of request & response parameters found:");
        for (Class c : paramsByType.keySet()) {
            System.out.println(c.getName());
        }
        System.out.println("\n---");
        for (Map.Entry<Class, Set<Field>> entry : paramsByType.entrySet()) {
            System.out.println("\nFields of type " + entry.getKey());
            System.out.println("---");
            for (Field f : entry.getValue()) {
                System.out.println("" + f.getDeclaringClass().getName() + ": " + f.getName());
            }
        }
    }
}
