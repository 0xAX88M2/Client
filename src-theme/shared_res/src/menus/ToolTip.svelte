<script>
    import {fade, fly} from "svelte/transition";
    import {afterUpdate} from "svelte";

    export let text;

    let element;
    let shown = false;

    afterUpdate(() => {
        element.parentNode.addEventListener("mouseenter", e => {
            shown = true;
        });

        element.parentNode.addEventListener("mouseleave", e => {
            shown = false;
        });
    });
</script>

<div bind:this={element}>
    {#if shown}
        <div transition:fly="{{ y: -10, duration: 200 }}" class="tooltip">{text}</div>
    {/if}
</div>

<style lang="scss">
  .tooltip {
    background-color: black;
    color: white;
    padding: 10px 15px;
    border-radius: 20px;
    font-size: 16px;
    font-weight: bold;
    position: absolute;
    white-space: nowrap;
    left: 50%;
    top: 0;
    transform: translate(-50%, -45px);
    z-index: 1000;

    &::after {
      content: "";
      display: block;
      height: 12px;
      width: 12px;
      background-color: black;
      position: absolute;
      left: 50%;
      transform: translate(-50%, 2px) rotate(45deg);
    }
  }
</style>