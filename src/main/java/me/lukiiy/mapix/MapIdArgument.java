package me.lukiiy.mapix;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class MapIdArgument implements CustomArgumentType.Converted<String, String> {
    @Override
    public @NotNull String convert(@NotNull String nativeType) {
        return nativeType;
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        var dir = Mapix.getInstance().getWorldsDir().listFiles(File::isDirectory);
        if (dir == null) return builder.buildFuture();

        Arrays.stream(dir).forEach(it -> {
            var name = it.getName();
            if (name.startsWith(".") || !name.toLowerCase().startsWith(builder.getRemainingLowerCase())) return;

            builder.suggest(name);
        });

        return builder.buildFuture();
    }
}