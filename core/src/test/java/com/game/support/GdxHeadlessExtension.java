package com.game.support;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Extensión JUnit5 que arranca una {@link HeadlessApplication} una sola vez por JVM e
 * instala un {@link GL20}/{@link GL30} mockeado, de modo que clases libGDX que tocan GL
 * ({@code Texture}, {@code SpriteBatch}, {@code Stage}) puedan instanciarse en tests sin
 * una ventana ni una GPU real.
 *
 * <p>Uso: {@code @ExtendWith(GdxHeadlessExtension.class)} en la clase de test.
 *
 * <p><b>Importante:</b> esta extensión debe correr ANTES de que se cargue cualquier clase
 * cuyos campos estáticos se inicialicen desde {@code Gdx.graphics} (p. ej.
 * {@code BlackScreen.VH_WIDTH/VH_HEIGHT}). Por eso boota el backend headless en
 * {@code beforeAll}. No se asertan píxeles (el GL es un mock): se asertan geometría,
 * estado y hitboxes.
 *
 * <p>Ver {@code .claude/skills/libgdx-testing-headless.skill.md} y
 * {@code docs/ARQUITECTURA-AUDITORIA.md} §6.2.
 */
public class GdxHeadlessExtension implements BeforeAllCallback {

    private static volatile boolean started = false;

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        if (started) {
            return;
        }
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = -1; // no arrancar el loop de render; los tests avanzan manualmente
        new HeadlessApplication(new ApplicationAdapter() { }, config);

        GL20 gl20 = mock(GL20.class);
        when(gl20.glGenTexture()).thenReturn(1);     // handle de textura no-cero
        when(gl20.glCreateProgram()).thenReturn(1);
        when(gl20.glCreateShader(org.mockito.ArgumentMatchers.anyInt())).thenReturn(1);

        Gdx.gl = gl20;
        Gdx.gl20 = gl20;
        Gdx.gl30 = mock(GL30.class);

        started = true;
    }
}
