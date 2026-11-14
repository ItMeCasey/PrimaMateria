// Dev fixture for the integration check (scripts/server_check.py with PM_GRADLE_ARGS=-Pviewers=modjs). Copied into run/modjs/server_scripts.
PrimaMateria.reset();
PrimaMateria.on('sifted', e => { if (e.input() == 'minecraft:gravel') e.addDrop('minecraft:stick', 7); });
PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:gold_nugget', chance: 0.02, mesh_min: 4 });
console.log('PM-SCRIPT loaded, max_load=' + PrimaMateria.config.get('sieve.max_load'));
