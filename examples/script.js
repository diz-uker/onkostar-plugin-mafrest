/*
 * MIT License
 *
 * Copyright (c) 2023 Comprehensive Cancer Center Mainfranken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

let onResponse = (resp) => {
    if (resp.status && resp.status.code !== 1) {
        console.warn(resp, 'Error requesting simple variants!');
        return;
    }

    let mapProps = (value) => {
        let result = [];
        result.val = value.val;
        result.version = value.version;
        return result;
    }

    let simpleVariants = resp.result.map((s) => {
        let result = s;
        result.Dokumentation = mapProps(s.Dokumentation);
        result.EVChromosom = mapProps(s.EVChromosom);
        result.Ergebnis = mapProps(s.Ergebnis);
        result.Untersucht = mapProps(s.Untersucht);
        return result;
    });

    // Erweiterte Dokumentation
    setFieldValue('Dokumentation', 'ERW');

    // Aktiviere "Sequenzierung"
    let methoden = new Set(getFieldValue('AnalyseMethoden').split(',').map(m => m.trim()));
    methoden.add('S');
    setFieldValue('AnalyseMethoden', Array.from(methoden).join(', '));

    setFieldValue('MolekulargenetischeUntersuchung', simpleVariants);
};

executePluginMethod(
    'MafRepoProcedureAnalyzer',
    'requestSimpleVariants',
    {sampleId: encodeURIComponent(getFieldValue('Einsendenummer'))},
    onResponse,
    false
);