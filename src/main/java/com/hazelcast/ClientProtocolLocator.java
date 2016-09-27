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

import com.hazelcast.nio.serialization.Data;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .build(new SubTypesScanner(false), new TypeAnnotationsScanner(), ClasspathHelper.forPackage("com.hazelcast")));

        Set<Class> requestResponseParamsClasses = reflections.getTypesAnnotatedWith(SuppressFBWarnings.class).stream()
        .filter(klass -> {
            return klass.getName().endsWith("Codec");
        }).flatMap(klass -> {
            return Arrays.stream(klass.getDeclaredClasses());
        }).filter(klass -> {
            return klass.getName().endsWith("Parameters");
        }).collect(Collectors.toSet());

        // Map: Enclosing Class -> its fields
        Map<Class, Set<Field>> paramsByType = new HashMap<>();
        // Map: Field name -> "enclosing class: field name", just for Data-typed parameters
        Map<String, Set<String>> dataParamsByParamName = new HashMap<>();
        for (Class klass : requestResponseParamsClasses) {
            for (Field f : klass.getDeclaredFields()) {
                // ignore static field named TYPE
                if (f.getName().equals("TYPE")) {
                    continue;
                }
                if (!paramsByType.containsKey(f.getType())) {
                    Set<Field> fields = new TreeSet<>(new Comparator<Field>() {
                        @Override
                        public int compare(Field o1, Field o2) {
                            if (o1 == null) {
                                return -1;
                            } else if (o2 == null) {
                                return 1;
                            } else {
                                if (o1.getDeclaringClass() == o2.getDeclaringClass()) {
                                    return o1.getName().compareTo(o2.getName());
                                } else {
                                    return o1.getDeclaringClass().getName().compareTo(o2.getDeclaringClass().getName());
                                }
                            }
                        }
                    });
                    paramsByType.put(f.getType(), fields);
                }
                paramsByType.get(f.getType()).add(f);
                if (f.getType().isAssignableFrom(Data.class)) {
                    if (!dataParamsByParamName.containsKey(f.getName())) {
                        dataParamsByParamName.put(f.getName(), new TreeSet<>());
                    }
                    dataParamsByParamName.get(f.getName()).add(toString(f));
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
                System.out.println(toString(f));
            }
        }

        for (Map.Entry<String, Set<String>> entry : dataParamsByParamName.entrySet()) {
            System.out.println("\nParameters with name " + entry.getKey());
            for (String s : entry.getValue()) {
                System.out.println("\t" + s);
            }
        }
    }

    private static String toString(Field f) {
        return f.getDeclaringClass().getName() + ": " + f.getName();
    }
}
