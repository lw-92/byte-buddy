package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderProcessorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;
    @Mock
    private MethodDescription source;

    @Mock
    private MethodDescription bindableTarget, unbindableTarget, dominantBindableTarget;
    @Mock
    private MethodDelegationBinder.Binding boundDelegation, unboundDelegation, dominantBoundDelegation;

    @Mock
    private MethodDelegationBinder methodDelegationBinder;
    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Before
    public void setUp() throws Exception {
        when(boundDelegation.isValid()).thenReturn(true);
        when(unboundDelegation.isValid()).thenReturn(false);
        when(dominantBoundDelegation.isValid()).thenReturn(true);
        when(methodDelegationBinder.bind(typeDescription, source, bindableTarget))
                .thenReturn(boundDelegation);
        when(methodDelegationBinder.bind(typeDescription, source, unbindableTarget))
                .thenReturn(unboundDelegation);
        when(methodDelegationBinder.bind(typeDescription, source, dominantBindableTarget))
                .thenReturn(dominantBoundDelegation);
        ambiguityResolver = mock(MethodDelegationBinder.AmbiguityResolver.class);
        when(ambiguityResolver.resolve(source, dominantBoundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        when(ambiguityResolver.resolve(source, boundDelegation, dominantBoundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        when(ambiguityResolver.resolve(source, boundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoBindableTarget() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, unbindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(typeDescription, source, methodDescriptions);
    }

    @Test
    public void testOneBindableTarget() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(boundDelegation));
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, unbindableTarget);
        verify(unboundDelegation, atLeast(2)).isValid();
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testTwoBindableTargetsWithDominant() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, unbindableTarget);
        verify(unboundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoBindableTargetsWithoutDominant() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(typeDescription, source, methodDescriptions);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableFirst() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(dominantBindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver, times(2)).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableMid() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(bindableTarget, dominantBindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verify(ambiguityResolver).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableLast() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(bindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, boundDelegation);
        verify(ambiguityResolver, times(2)).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }
}
