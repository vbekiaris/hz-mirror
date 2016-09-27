package com.hazelcast;

import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spi.Operation;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class OperationsLocator {

    public static void main(String[] args) {

        org.reflections.Reflections reflections = new Reflections(
                new ConfigurationBuilder().forPackages("com.hazelcast").addScanners(new SubTypesScanner()).build());

        Collection allOperationClasses = reflections.getSubTypesOf(Operation.class);
        Collection allIDSClasses = reflections.getSubTypesOf(IdentifiedDataSerializable.class);

        boolean itemsRemoved = allOperationClasses.removeAll(allIDSClasses);

        if (!itemsRemoved) {
            System.out.println("Hooray! All operation classes implement IdentifiedDataSerializable!");
        } else {
            Set<String> classNames = new TreeSet<>();
            for (Object o : allOperationClasses) {
                classNames.add(o.toString());
            }
            int i = 0;
            for (String s : classNames) {
                i++;
                System.out.println(i + "\t" + s);
            }
        }
    }


}
