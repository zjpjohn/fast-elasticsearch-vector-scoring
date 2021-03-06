package com.liorkn.elasticsearch.service;

import com.google.common.collect.ImmutableMap;
import com.liorkn.elasticsearch.script.VectorScoreScript;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.LeafSearchScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Lior Knaany on 5/14/17.
 */
public class VectorScoringScriptEngineService extends AbstractComponent implements ScriptEngineService{

    public static final String NAME = "knn";

    private final ImmutableMap<String, NativeScriptFactory> scripts;


    @Inject
    public VectorScoringScriptEngineService(Settings settings, Map<String, NativeScriptFactory> scripts) {
        super(settings);
        this.scripts = ImmutableMap.copyOf(scripts);
    }

    @Override
    public String[] types() {
        return new String[]{NAME};
    }

    @Override
    public String[] extensions() {
        return new String[]{NAME};
    }

    @Override
    public boolean sandboxed() {
        return false;
    }

    @Override
    public Object compile(String script, Map<String, String> params) {
        NativeScriptFactory scriptFactory = scripts.get(script);
        if (scriptFactory != null) {
            return scriptFactory;
        }
        throw new IllegalArgumentException("knn script [" + script + "] not found");
    }

    @Override
    public ExecutableScript executable(CompiledScript compiledScript, @Nullable Map<String, Object> vars) {
        NativeScriptFactory scriptFactory = (NativeScriptFactory) compiledScript.compiled();
        return scriptFactory.newScript(vars);
    }

    @Override
    public SearchScript search(CompiledScript compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory) compiledScript.compiled();
        final VectorScoreScript script = (VectorScoreScript) scriptFactory.newScript(vars);
        return new SearchScript() {
            @Override
            public LeafSearchScript getLeafSearchScript(LeafReaderContext context) throws IOException {
                script.setBinaryEmbeddingReader(context.reader().getBinaryDocValues(script.field));
                return script;
            }
            @Override
            public boolean needsScores() {
                return scriptFactory.needsScores();
            }
        };
    }

    @Override
    public void close() {
    }
}
