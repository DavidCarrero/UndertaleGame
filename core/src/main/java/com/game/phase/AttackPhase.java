package com.game.phase;

/**
 * Patrón State para las fases de ataque (los "actos") de la pelea de Sans.
 *
 * Cada fase del combate implementa esta interfaz. Reemplaza el antiguo
 * {@code switch (act)} de BattleController: el motor de fases llama
 * {@link #onEnter()} una vez al entrar, {@link #update(float)} cada frame
 * mientras la fase está activa, y cuando {@link #isFinished()} devuelve true
 * transiciona a la fase indicada por {@link #nextPhase()}.
 *
 * <p>Para añadir un nivel nuevo: crear una clase que implemente AttackPhase y
 * registrarla en el motor de fases (PhaseMachine), sin tocar las demás fases.
 */
public interface AttackPhase {

    /** Se llama una vez al entrar en la fase: inicializa box, heart.option, listas, etc. */
    void onEnter();

    /** Se llama cada frame mientras la fase está activa. {@code delta} = tiempo desde el último frame. */
    void update(float delta);

    /** true cuando la fase ha terminado y debe transicionarse a la siguiente. */
    boolean isFinished();

    /** Se llama una vez al salir de la fase: limpia proyectiles/estado propios. */
    default void onExit() { }

    /** Nombre legible para depuración/registro. */
    default String name() { return getClass().getSimpleName(); }
}
