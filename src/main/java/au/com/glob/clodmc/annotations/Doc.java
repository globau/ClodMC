package au.com.glob.clodmc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@NullMarked
public @interface Doc {
  Audience audience();

  String title();

  String description();

  boolean hidden() default false;
}
